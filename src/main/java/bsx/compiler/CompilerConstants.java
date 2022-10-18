package bsx.compiler;

import bsx.BsType;
import bsx.BsValue;
import bsx.compiler.jvm.util.ClassData;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.compiler.lvt.Scope;
import bsx.invoke.BlockScopes;
import bsx.invoke.Types;
import bsx.invoke.Values;
import bsx.type.*;
import bsx.util.Bytecode;
import bsx.util.string.StringEscapeHelper;
import bsx.value.*;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompilerConstants {
    
    private static final Map<BsType, Integer> PRIMITIVE_TYPES = Stream.<Map.Entry<BsType, Integer>>of(
            Map.entry(AnyType.INSTANCE, 0),
            Map.entry(NoValue.INSTANCE, 1),
            Map.entry(NullValue.NULL, 10),
            Map.entry(NullValue.NOTHING, 11),
            Map.entry(NullValue.UNDEFINED, 12),
            Map.entry(NullValue.NADA, 13),
            Map.entry(NullValue.EMPTY, 14),
            Map.entry(BoolType.INSTANCE, 20),
            Map.entry(IntegerType.INSTANCE, 21),
            Map.entry(FloatType.INSTANCE, 22),
            Map.entry(StringType.ASCII, 30),
            Map.entry(StringType.ANSI, 31),
            Map.entry(StringType.DBCS, 32),
            Map.entry(StringType.EBCDIC, 33),
            Map.entry(StringType.UTF256, 34)
    ).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    
    private static final Map<BsValue, Integer> PRIMITIVE_VALUES = Stream.<Map.Entry<BsValue, Integer>>of(
            Map.entry(NoValue.INSTANCE, 0),
            Map.entry(NullValue.NULL, 10),
            Map.entry(NullValue.NOTHING, 11),
            Map.entry(NullValue.UNDEFINED, 12),
            Map.entry(NullValue.NADA, 13),
            Map.entry(NullValue.EMPTY, 14),
            Map.entry(BoolValue.FALSE, 20),
            Map.entry(BoolValue.TRUE, 21)
    ).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    
    private static Object typeConstant(BsType type) {
        return typeConstant(type, internal -> false);
    }
    
    public static Object typeConstant(CompilerContext ctx, BsType type) {
        return typeConstant(ctx.data(), type);
    }
    
    public static Object typeConstant(ClassData data, BsType type) {
        return typeConstant(type, data::exists);
    }
    
    private static Object typeConstant(BsType type, Predicate<String> internalNameExists) {
        if (type instanceof ArrayType arrayType) {
            return arrayTypeConstant(typeConstant(arrayType.elementType(), internalNameExists));
        } else if (type instanceof ClassType classType) {
            String internal = classType.typeName().replace('.', '/');
            try {
                Class.forName(classType.typeName());
            } catch (ClassNotFoundException e) {
                if (!internalNameExists.test(internal)) {
                    throw new IllegalStateException("Unknown type: " + internal, e);
                }
            }
            return classTypeConstant(Type.getObjectType(internal));
        } else if (PRIMITIVE_TYPES.containsKey(type)) {
            return new ConstantDynamic(
                    "type", Type.getType(BsType.class).getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Types.class.getMethod("primitiveType", MethodHandles.Lookup.class, String.class, Class.class, int.class)),
                    PRIMITIVE_TYPES.get(type)
            );
        } else {
            throw new IllegalArgumentException("Non-constant type: " + type);
        }
    }

    public static Object arrayTypeConstant(Object elementConstant) {
        return new ConstantDynamic(
                "type", Type.getType(BsType.class).getDescriptor(),
                Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Types.class.getMethod("arrayType", MethodHandles.Lookup.class, String.class, Class.class, BsType.class)),
                elementConstant
        );
    }
    
    public static Object classTypeConstant(Type type) {
        if (type.getSort() == Type.ARRAY) {
            Object theType = classTypeConstant(type.getElementType());
            for (int i = 0; i < type.getDimensions(); i++) {
                theType = arrayTypeConstant(theType);
            }
            return theType;
        } else {
            return new ConstantDynamic(
                    "type", Type.getType(BsType.class).getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Types.class.getMethod("classType", MethodHandles.Lookup.class, String.class, Class.class, Class.class)),
                    type
            );
        }
    }
    
    public static Object valueConstant(BsValue value) {
        if (value instanceof IntegerValue iv) {
            return new ConstantDynamic(
                    "value", Type.getType(BsValue.class).getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeIntegerValue", MethodHandles.Lookup.class, String.class, Class.class, int.class)),
                    iv.value
            );
        } else if (value instanceof FloatingValue fv) {
            return new ConstantDynamic(
                    "value", Type.getType(BsValue.class).getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeFloatingValue", MethodHandles.Lookup.class, String.class, Class.class, double.class)),
                    fv.value
            );
        } else if (value instanceof StringValue sv) {
            if (sv.getPrintableString().isEmpty()) {
                throw new IllegalArgumentException("Can't make non printable (utf256) string constant from built string.");
            } else {
                return new ConstantDynamic(
                        "value", Type.getType(BsValue.class).getDescriptor(),
                        Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeStringValue", MethodHandles.Lookup.class, String.class, Class.class, BsType.class, String.class)),
                        typeConstant(sv.getType()),
                        sv.getPrintableString().orElseThrow()
                );
            }
        } else if (PRIMITIVE_VALUES.containsKey(value)) {
            return new ConstantDynamic(
                    "value", Type.getType(BsValue.class).getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makePrimitiveValue", MethodHandles.Lookup.class, String.class, Class.class, int.class)),
                    PRIMITIVE_VALUES.get(value)
            );
        } else {
            throw new IllegalArgumentException("Non-constant value: " + value + " (of type " + value.getType() + ")");
        }
    }
    
    public static Object utfConstant(String escapedString) {
        StringEscapeHelper.unescapeUtf256(escapedString);
        return new ConstantDynamic(
                "value", Type.getType(BsValue.class).getDescriptor(),
                Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeUtf256Value", MethodHandles.Lookup.class, String.class, Class.class, String.class)),
                escapedString
        );
    }
    
    public static InvokeDynamicInsnNode utfFactory(String escapedString, int numEscapes) {
        String[] constantParts = escapedString.split("\\$", -1);
        if (constantParts.length != numEscapes + 1) {
            throw new IllegalArgumentException("Escape mismatch: expected " + numEscapes + ", template string has + " + (constantParts.length - 1));
        }
        for (String constantPart : constantParts) {
            StringEscapeHelper.unescapeUtf256(constantPart);
        }
        Type[] argTypes = new Type[numEscapes];
        for (int i = 0; i < numEscapes; i++) {
            argTypes[i] = Type.getType(BsValue.class);
        }
        return new InvokeDynamicInsnNode(
                "interpolate", Type.getMethodDescriptor(Type.getType(BsValue.class), argTypes),
                Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeUtf256ValueInterpolation", MethodHandles.Lookup.class, String.class, MethodType.class, String.class)),
                escapedString
        );
    }
    
    public static InsnList takeSnapshot(BlockScope scope) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(new ConstantDynamic(
                "scope", Type.getType(Scope.class).getDescriptor(),
                Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> BlockScopes.class.getMethod("blockScope", MethodHandles.Lookup.class, String.class, Class.class, int.class, String.class, String.class)),
                scope.offset, String.join("\0", scope.allVariables()), String.join("\0", scope.deletedVariables().stream().sorted().toList())
        )));
        instructions.add(scope.createVarArray());
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> BlockScopes.class.getMethod("setEvaluationSnapshot", Scope.class, Object[].class)));
        return instructions;
    }
}
