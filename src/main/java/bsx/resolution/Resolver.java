package bsx.resolution;

import bsx.BSX;
import bsx.BsValue;
import bsx.util.ValueHelper;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Resolver {
    
    public static final String SPECIAL_NAME = "BS_RESOLVE";
    
    @Nullable
    public static MethodHandle resolve(Class<?> target, String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        return resolve(target, target, name, args, instance, special);
    }
    
    @Nullable
    public static MethodHandle resolve(Class<?> target, Class<?> cls, String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        MethodHandle specialHandle = findSpecial(cls, name, args, instance, special);
        if (specialHandle != null) return specialHandle;
        
        boolean isSuper = name.startsWith("super@");
        name = isSuper ? name.substring(6) : name;

        if ((instance || isSuper || name.equals("__construct")) && target.getAnnotation(Singleton.class) != null) {
            return null;
        }
        
        if (cls == target) {
            List<Class<?>> ifaces = new ArrayList<>();
            Class<?> current = isSuper ? cls.getSuperclass() : cls;
            while (current != null) {
                MethodHandle result = findIn(target, current, name, args, instance, special, target == current, isSuper);
                if (result != null) return result;
                for (Class<?> iface : cls.getInterfaces()) {
                    if (!ifaces.contains(iface)) ifaces.add(iface);
                }
                current = current.getSuperclass();
            }
            for (int i = 0; i < ifaces.size(); i++) {
                MethodHandle result = findIn(target, ifaces.get(i), name, args, instance, special, false, isSuper);
                if (result != null) return result;
                for (Class<?> iface : cls.getInterfaces()) {
                    if (!ifaces.contains(iface)) ifaces.add(iface);
                }
            }
            return null;
        } else {
            return findIn(target, cls, name, args, instance, special, true, false);
        }
    }
    
    @Nullable
    private static MethodHandle findSpecial(Class<?> cls, String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        try {
            Method method = cls.getDeclaredMethod(SPECIAL_NAME, String.class, List.class, boolean.class, boolean.class);
            if (!Modifier.isStatic(method.getModifiers()) || !MethodHandle.class.isAssignableFrom(method.getReturnType())) return null;
            method.setAccessible(true);
            return (MethodHandle) method.invoke(null, name, args, instance, special);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    @Nullable
    private static MethodHandle findIn(Class<?> target, Class<?> cls, String name, List<BsValue> args, boolean instance, boolean special, boolean canFindStaticsAsInstance, boolean isSuper) throws ReflectiveOperationException {
        List<MethodHandle> allPossible = new ArrayList<>();
        if (name.equals("__construct")) {
            if (special || instance || cls.isInterface() || cls != target || isSuper) return null;
            
            // For java classes (that have not been compiled by BSX
            for (Constructor<?> ctor : cls.getConstructors()) {
                if (ctor.isAnnotationPresent(NoLookup.class)) continue;
                allPossible.add(MethodHandles.lookup().unreflectConstructor(ctor));
            }
            
            // BSX compiled classes use a no-arg constructor and an instance method named __construct
            Constructor<?> noArgCtor;
            try {
                // Don't check @NoLookup, BSX classes use this on their no-arg constructor to prevent it from being
                // resolved directly
                noArgCtor = cls.getConstructor();
            } catch (NoSuchMethodException e) {
                noArgCtor = null;
            }
            if (noArgCtor != null) {
                for (Method method : cls.getMethods()) {
                    if (method.isAnnotationPresent(NoLookup.class)) continue;
                    if (!method.getName().equals("__construct")) continue;
                    if (Modifier.isStatic(method.getModifiers())) continue;
                    if (method.isAnnotationPresent(SpecialInvoke.class)) continue;
                    if (canInvokeAs(true, method, target, false)) {
                        MethodHandle realConstructor = MethodHandles.lookup().unreflectConstructor(noArgCtor);
                        MethodHandle instanceCall = MethodHandles.lookup().unreflect(method);
                        MethodType targetType = instanceCall.type().dropParameterTypes(0, 1).changeReturnType(realConstructor.type().returnType());
                        MethodHandle ctorChain = MethodHandles.lookup().findStatic(Resolver.class, "constructorWithInitialisation", MethodType.methodType(Object.class, MethodHandle.class, MethodHandle.class, Object[].class));
                        MethodHandle withHandles = MethodHandles.insertArguments(ctorChain, 0, realConstructor, instanceCall);
                        MethodHandle collector = withHandles.asCollector(Object[].class, targetType.parameterCount());
                        allPossible.add(MethodHandles.explicitCastArguments(collector, targetType));
                    }
                }
            }
        } else {
            int instanceLen = instance ? 1 : 0;
            if (special) {
                if (!isSuper) {
                    if (args.size() == instanceLen) {
                        for (Field field : cls.getFields()) {
                            if (field.isAnnotationPresent(NoLookup.class)) continue;
                            if (field.getName().equals(SPECIAL_NAME)) continue;
                            if (instance == Modifier.isStatic(field.getModifiers())) continue;
                            if (!Objects.equals(field.getName(), name)) continue;
                            allPossible.add(MethodHandles.lookup().unreflectGetter(field));
                        }
                    } else if (args.size() == instanceLen + 1) {
                        for (Field field : cls.getFields()) {
                            if (field.isAnnotationPresent(NoLookup.class)) continue;
                            if (field.getName().equals(SPECIAL_NAME)) continue;
                            if (instance == Modifier.isStatic(field.getModifiers())) continue;
                            if (Modifier.isFinal(field.getModifiers())) continue;
                            if (!Objects.equals(field.getName(), name)) continue;
                            allPossible.add(MethodHandles.lookup().unreflectSetter(field));
                        }
                    }
                    for (Method method : cls.getMethods()) {
                        if (method.isAnnotationPresent(NoLookup.class)) continue;
                        if (method.getName().equals(SPECIAL_NAME)) continue;
                        if (!method.isAnnotationPresent(SpecialInvoke.class)) continue;
                        if (!Objects.equals(method.getName(), name)) continue;
                        if (canInvokeAs(instance, method, target, canFindStaticsAsInstance)) {
                            allPossible.add(MethodHandles.lookup().unreflect(method));
                        }
                    }
                }
            } else {
                for (Method method : cls.getMethods()) {
                    if (method.isAnnotationPresent(NoLookup.class)) continue;
                    if (method.getName().equals(SPECIAL_NAME)) continue;
                    if (method.isAnnotationPresent(SpecialInvoke.class)) continue;
                    if (!Objects.equals(method.getName(), name)) continue;
                    if (canInvokeAs(instance, method, target, canFindStaticsAsInstance)) {
                        if (isSuper) {
                            MethodHandle handle = MethodHandles.lookup().unreflect(method);
                            // Use privileged lookup to get INVOKESPECIAL handle
                            Class<?>[] argTypes = new Class<?>[Modifier.isStatic(method.getModifiers()) ? handle.type().parameterCount() : handle.type().parameterCount() - 1];
                            System.arraycopy(handle.type().parameterArray(), Modifier.isStatic(method.getModifiers()) ? 0 : 1, argTypes, 0, argTypes.length);
                            allPossible.add(BSX.LOOKUP.findSpecial(cls, method.getName(), MethodType.methodType(handle.type().returnType(), argTypes), target));
                        } else {
                            allPossible.add(MethodHandles.lookup().unreflect(method));
                        }
                    }
                }
            }
        }
        return selectBy(allPossible, args, instance);
    }
    
    private static boolean canInvokeAs(boolean instance, Method method, Class<?> target, boolean canFindStaticsAsInstance) {
        if (instance) {
            if (!Modifier.isStatic(method.getModifiers())) {
                return method.getDeclaringClass().isAssignableFrom(target);
            } else if (canFindStaticsAsInstance) {
                return method.isAnnotationPresent(InvokeAsInstanceMethod.class) && method.getParameterTypes().length >= 1 && method.getParameterTypes()[0].isAssignableFrom(target);
            } else {
                return false;
            }
        } else {
            return Modifier.isStatic(method.getModifiers()) && !method.isAnnotationPresent(InvokeAsInstanceMethod.class);
        }
    }
    
    @Nullable
    private static MethodHandle selectBy(List<MethodHandle> handles, List<BsValue> args, boolean instance) throws NoSuchMethodException, IllegalAccessException {
        List<MethodHandle> fixed = new ArrayList<>();
        List<MethodHandle> variadic = new ArrayList<>();
        for (MethodHandle handle : handles) {
            if (handle.isVarargsCollector()) {
                if (handle.type().parameterCount() <= args.size()) variadic.add(fixUpTo(handle, args.size()));
            } else {
                if (handle.type().parameterCount() == args.size()) fixed.add(handle);
            }
        }
        // Non-variadic methods are found first
        MethodHandle nonVariadic = selectFrom(fixed, args, instance);
        if (nonVariadic != null) return nonVariadic;
        return selectFrom(variadic, args, instance);
    }
    
    // All method handles will have exact the same arg count as the list size
    @Nullable
    private static MethodHandle selectFrom(List<MethodHandle> handles, List<BsValue> args, boolean instance) throws NoSuchMethodException, IllegalAccessException {
        if (handles.size() == 0) {
            return null;
        } else if (handles.size() == 1) {
            return withValueArgs(handles.get(0), instance);
        } else {
            // Ambiguous (overloaded) methods. Try to find a single matching one.
            List<MethodHandle> matching = new ArrayList<>();
            for (MethodHandle handle : handles) {
                if (matchesAll(handle, args)) {
                    matching.add(handle);
                }
            }
            if (matching.size() == 1) {
                return matching.get(0);
            } else {
                // None or multiple. Nothing we can do.
                return null;
            }
        }
    }
    
    @Nullable
    private static MethodHandle fixUpTo(MethodHandle variadic, int totalArgs) {
        return variadic.asCollector(variadic.type().parameterType(variadic.type().parameterCount() - 1), totalArgs - variadic.type().parameterCount() + 1);
    }
    
    private static boolean matchesAll(MethodHandle handle, List<BsValue> args) {
        try {
            for (int i = 0; i < args.size(); i++) {
                if (!args.get(i).matchesJava(handle.type().parameterType(i))) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Adjust the method handle such that all args to accept objects of type Value
    // that are then converted into the required type.
    private static MethodHandle withValueArgs(MethodHandle handle, boolean instance) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle[] filters = new MethodHandle[handle.type().parameterCount()];
        for (int i = 0; i < handle.type().parameterCount(); i++) {
            filters[i] = ValueHelper.toJava(handle.type().parameterType(i));
        }
        return MethodHandles.filterArguments(handle, 0, filters);
    }
    
    private static Object constructorWithInitialisation(MethodHandle constructor, MethodHandle initialisation, Object[] args) throws Throwable {
        Object value = constructor.invoke();
        initialisation.bindTo(value).asSpreader(Object[].class, args.length).invoke(args);
        return value;
    }
}
