package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.AnyType;
import bsx.type.FloatType;
import bsx.type.IntegerType;

public class IntegerValue implements BsValue {
    
    public final int value;

    public IntegerValue(int value) {
        // Check the 17th bit for the sign to support int overflow
        // Negative numbers that are in bound will have it set anyway
        if ((value & 0x00010000) == 0) {
            // Positive number, fill with zeros and clear sign bit
            this.value = value & 0x0000FFFF;
        } else {
            // Negative number, fill with ones and set sign bit
            this.value = value | 0xFFFF0000;
        }
    }

    @Override
    public BsType getType() {
        return IntegerType.INSTANCE;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || cls == Byte.class || cls == Short.class || cls == Integer.class || cls == Long.class || cls == Float.class || cls == Double.class || cls == Object.class;
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == Byte.class) {
            //noinspection unchecked
            return (T) Byte.valueOf((byte) this.value);
        } else if (cls == Short.class) {
            //noinspection unchecked
            return (T) Short.valueOf((short) this.value);
        } else if (cls == Integer.class || cls == Object.class) {
            //noinspection unchecked
            return (T) Integer.valueOf(this.value);
        } else if (cls == Long.class) {
            //noinspection unchecked
            return (T) Long.valueOf(this.value);
        } else if (cls == Float.class) {
            //noinspection unchecked
            return (T) Float.valueOf(this.value);
        } else if (cls == Double.class) {
            //noinspection unchecked
            return (T) Double.valueOf(this.value);
        } else {
            return BsValue.super.asJava(cls);
        }
    }

    @Override
    public boolean isOf(BsType type) {
        return type == AnyType.INSTANCE || type == IntegerType.INSTANCE || type == FloatType.INSTANCE;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.value); // int and double can be equal
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerValue iv) {
            return this.value == iv.value;
        } else if (obj instanceof FloatingValue fv) {
            return this.value == fv.value;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }
}
