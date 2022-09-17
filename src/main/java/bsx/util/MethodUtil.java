package bsx.util;

import bsx.BsValue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.IntStream;

public class MethodUtil {
    
    public static MethodHandle boundThis(MethodHandle handle, Object self) {
        MethodHandle noSelf = MethodHandles.insertArguments(handle, 0, self);
        // Let the caller provide a this reference that is ignored
        return MethodHandles.dropArguments(noSelf, 0, Object.class);
    }
    
    public static MethodHandle resolveSimpleCall(MethodHandle handle, int args) {
        return MethodHandles.dropArguments(handle, 0, IntStream.range(0, args).<Class<?>>mapToObj(e -> BsValue.class).toList());
    }
    
    public static MethodHandle wrapReturnToObject(MethodHandle handle) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle wrapped = wrapReturn(handle);
        if (wrapped.type().returnType() == void.class) {
            return wrapped;
        } else {
            return wrapped.asType(wrapped.type().changeReturnType(Object.class));
        }
    }
    
    public static MethodHandle wrapReturn(MethodHandle handle) throws NoSuchMethodException, IllegalAccessException {
        Class<?> rtype = handle.type().returnType();
        if (rtype == boolean.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Boolean.class, "valueOf", MethodType.methodType(Boolean.class, boolean.class)));
        } else if (rtype == byte.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Byte.class, "valueOf", MethodType.methodType(Byte.class, byte.class)));
        } else if (rtype == char.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Character.class, "valueOf", MethodType.methodType(Character.class, char.class)));
        } else if (rtype == short.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Short.class, "valueOf", MethodType.methodType(Short.class, short.class)));
        } else if (rtype == int.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, int.class)));
        } else if (rtype == long.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Long.class, "valueOf", MethodType.methodType(Long.class, long.class)));
        } else if (rtype == float.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Float.class, "valueOf", MethodType.methodType(Float.class, float.class)));
        } else if (rtype == double.class) {
            return MethodHandles.filterReturnValue(handle, MethodHandles.publicLookup().findStatic(Double.class, "valueOf", MethodType.methodType(Double.class, double.class)));
        } else {
            return handle;
        }
    }
    
    public static MethodHandle wrapper(Class<?> cls) throws NoSuchMethodException, IllegalAccessException {
        if (cls == boolean.class) {
            return MethodHandles.publicLookup().findStatic(Boolean.class, "valueOf", MethodType.methodType(Boolean.class, boolean.class));
        } else if (cls == byte.class) {
            return MethodHandles.publicLookup().findStatic(Byte.class, "valueOf", MethodType.methodType(Byte.class, byte.class));
        } else if (cls == char.class) {
            return MethodHandles.publicLookup().findStatic(Character.class, "valueOf", MethodType.methodType(Character.class, char.class));
        } else if (cls == short.class) {
            return MethodHandles.publicLookup().findStatic(Short.class, "valueOf", MethodType.methodType(Short.class, short.class));
        } else if (cls == int.class) {
            return MethodHandles.publicLookup().findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, int.class));
        } else if (cls == long.class) {
            return MethodHandles.publicLookup().findStatic(Long.class, "valueOf", MethodType.methodType(Long.class, long.class));
        } else if (cls == float.class) {
            return MethodHandles.publicLookup().findStatic(Float.class, "valueOf", MethodType.methodType(Float.class, float.class));
        } else if (cls == double.class) {
            return MethodHandles.publicLookup().findStatic(Double.class, "valueOf", MethodType.methodType(Double.class, double.class));
        } else {
            return MethodHandles.identity(cls);
        }
    }
    
    public static MethodHandle unwrapper(Class<?> cls) throws NoSuchMethodException, IllegalAccessException {
        if (cls == boolean.class) {
            return MethodHandles.publicLookup().findVirtual(Boolean.class, "booleanValue", MethodType.methodType(boolean.class));
        } else if (cls == byte.class) {
            return MethodHandles.publicLookup().findVirtual(Byte.class, "byteValue", MethodType.methodType(byte.class));
        } else if (cls == char.class) {
            return MethodHandles.publicLookup().findVirtual(Character.class, "charValue", MethodType.methodType(char.class));
        } else if (cls == short.class) {
            return MethodHandles.publicLookup().findVirtual(Short.class, "shortValue", MethodType.methodType(short.class));
        } else if (cls == int.class) {
            return MethodHandles.publicLookup().findVirtual(Integer.class, "intValue", MethodType.methodType(int.class));
        } else if (cls == long.class) {
            return MethodHandles.publicLookup().findVirtual(Long.class, "longValue", MethodType.methodType(long.class));
        } else if (cls == float.class) {
            return MethodHandles.publicLookup().findVirtual(Float.class, "floatValue", MethodType.methodType(float.class));
        } else if (cls == double.class) {
            return MethodHandles.publicLookup().findVirtual(Double.class, "doubleValue", MethodType.methodType(double.class));
        } else {
            return MethodHandles.identity(cls);
        }
    }
}
