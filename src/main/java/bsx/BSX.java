package bsx;

import bsx.compiler.CompiledProgram;
import bsx.compiler.CompilerAPI;
import bsx.compiler.ast.Program;
import bsx.compiler.lvt.Scope;
import bsx.invoke.Calls;
import bsx.util.MethodUtil;
import bsx.util.StackTraceCleaner;
import bsx.value.NoValue;
import bsx.value.NullValue;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class BSX {
    
    public static final String GREEK_QUESTION_MARK = "\u037E";
    public static final String PIE = "🥧";
    public static final String REPLACEMENT_CHAR = "�";
    
    public static final Unsafe UNSAFE = getTheUnsafe();
    public static final MethodHandles.Lookup LOOKUP = getTheLookup(); // Needed to resolve INVOKESPECIAL for super calls
    
    public static final ThreadLocal<EvaluationSnapshot> SNAPSHOT = ThreadLocal.withInitial(() -> new EvaluationSnapshot(Scope.EMPTY, new Object[0]));

    public static void setEvaluationSnapshot(Scope scope, Object[] variables) {
        SNAPSHOT.set(new EvaluationSnapshot(scope, variables));
    }
    
    public static void invoke(MethodHandle block, BsValue... args) {
        try {
            block.invokeWithArguments((Object[]) args);
        } catch (Throwable e) {
            UNSAFE.throwException(e);
        }
    }
    
    public static void invokeWithErrorHandling(MethodHandle block, BsValue... args) {
        try {
            block.invokeWithArguments((Object[]) args);
        } catch (Throwable t) {
            // HALT_AND_CATCH_FIRE should cause everything to die.
            StackTraceCleaner.process(t);
            t.printStackTrace();
            System.exit(1);
        }
    }
    
    public static MethodHandle resolve(NullValue value, String name, List<BsValue> args, boolean special) {
        return resolve((BsValue) value, name, args, special);
    }
    
    public static MethodHandle resolve(BsType type, String name, List<BsValue> args, boolean special) {
        return resolve(type, name, List.copyOf(args), false, special);
    }
    
    public static MethodHandle resolve(BsValue value, String name, List<BsValue> args, boolean special) {
        return resolve(value.getType(), name, Stream.concat(Stream.of(value), args.stream()).toList(), true, special);
    }

    private static MethodHandle resolve(BsType type, String name, List<BsValue> args, boolean instance, boolean special) {
        MethodHandle handle;
        try {
            handle = type.resolve(name, args, instance, special);
        } catch (NoSuchMethodException e) {
            handle = null;
        } catch (Exception e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
        if (handle == null) {
            if (!special && !name.isEmpty()) {
                // Could be a resolution of a special element followed by a call to empty name method
                try {
                    return resolveImplicitSpecialApplication(type, name, args, instance);
                } catch (Exception e) {
                    //
                }
            }
            throw new NoSuchElementException(type + (instance ? "->" : "::") + name + " is unbound");
        }
        try {
            handle = MethodUtil.wrapReturnToObject(handle);
            if (handle.type().returnType() == void.class) {
                return MethodHandles.filterReturnValue(handle, MethodHandles.lookup().findStatic(BSX.class, "voidFilter", MethodType.methodType(BsValue.class)));
            } else {
                return MethodHandles.filterReturnValue(handle, MethodHandles.lookup().findStatic(BsValue.class, "wrap", MethodType.methodType(BsValue.class, Object.class)));
            }
        } catch (Exception e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    private static MethodHandle resolveImplicitSpecialApplication(BsType type, String name, List<BsValue> args, boolean instance) {
        List<BsValue> specialResolveArgs;
        BsValue[] applicationArgs;
        if (instance) {
            specialResolveArgs = List.copyOf(args.subList(0, 1));
            applicationArgs = args.subList(1, args.size()).toArray(BsValue[]::new);
        } else {
            specialResolveArgs = List.of();
            applicationArgs = args.toArray(BsValue[]::new);
        }
        try {
            MethodHandle base = resolve(type, name, specialResolveArgs, instance, true);
            MethodHandle callHandle = MethodHandles.publicLookup().findStatic(Calls.class, "callValue", MethodType.methodType(BsValue.class, BsValue.class, String.class, Object[].class, boolean.class));
            MethodHandle returnMappingHandle = MethodHandles.insertArguments(callHandle, 1, "", applicationArgs, false);
            return MethodHandles.filterReturnValue(base, returnMappingHandle);
        } catch (Exception e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static void evaluate(String bs) {
        EvaluationSnapshot snapshot = SNAPSHOT.get();
        
        CompilerAPI api = CompilerAPI.get();
        String preprocessed = api.preprocess(bs);
        Program program = api.parseAST(preprocessed);
        CompiledProgram compiled = api.compile(program, snapshot.scope());
        MethodHandle handle = compiled.loadIntoCurrentEnvironment();

        try {
            handle.invokeWithArguments(snapshot.variables());
        } catch (Throwable e) {
            UNSAFE.throwException(e);
        }
    }
    
    private static BsValue voidFilter() {
        return NoValue.INSTANCE;
    }
    
    private static Unsafe getTheUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the unsafe", e);
        }
    }
    
    private static MethodHandles.Lookup getTheLookup() {
        try {
            Constructor<MethodHandles.Lookup> ctor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);

            Module base = Object.class.getModule();
            if (base.isNamed() && base.getDescriptor() != null) {
                // Temporarily open java.base to work around module limitations
                Field openField = ModuleDescriptor.class.getDeclaredField("open");
                long offset = UNSAFE.objectFieldOffset(openField);
                boolean oldValue = UNSAFE.getBoolean(base.getDescriptor(), offset);
                UNSAFE.putBoolean(base.getDescriptor(), offset, true);
                ctor.setAccessible(true);
                UNSAFE.putBoolean(base.getDescriptor(), offset, oldValue);
            } else {
                ctor.setAccessible(true);
            }

            return ctor.newInstance(Object.class, null, 0xFFFFFFFF);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the privileged lookup.", e);
        }
    }
    
    private record EvaluationSnapshot(Scope scope, Object[] variables) {}
}
