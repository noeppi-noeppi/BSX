package bsx.util;

import bsx.BsValue;
import bsx.value.FloatingValue;
import bsx.value.IntegerValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class ValueHelper {
    
    public static OptionalInt getIntegral(BsValue value) {
        return getIntegral(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    public static OptionalInt getIntegral(BsValue value, int min, int max) {
        if (value instanceof IntegerValue iv) {
            if (iv.value >= min && iv.value <= max) {
                return OptionalInt.of(iv.value);
            }
        } else if (value instanceof FloatingValue fv) {
            if (((int) fv.value) == fv.value && fv.value >= min && fv.value <= max) {
                return OptionalInt.of((int) fv.value);
            }
        }
        return OptionalInt.empty();
    }
    
    public static String fromCodePoints(IntStream codePoints) {
        StringBuilder sb = new StringBuilder();
        codePoints.forEach(sb::appendCodePoint);
        return sb.toString();
    }
    
    // MethodHandle: Value -> T
    @Nullable
    public static MethodHandle toJava(Class<?> cls) throws NoSuchMethodException, IllegalAccessException {
        if (cls == BsValue.class) return null;
        Class<?> boxed = ClassUtil.boxed(cls);
        MethodHandle handle = MethodHandles.lookup().findVirtual(BsValue.class, "asJava", MethodType.methodType(Object.class, Class.class));
        MethodHandle withCls = MethodHandles.insertArguments(handle, 1, boxed); // Pos 1 as 0 is this reference
        MethodHandle withCastedReturnValue = MethodHandles.explicitCastArguments(withCls, MethodType.methodType(boxed, BsValue.class));
        if (boxed != cls) {
            return MethodHandles.filterReturnValue(withCastedReturnValue, MethodUtil.unwrapper(cls));
        } else {
            return withCastedReturnValue;
        }
    }
}
