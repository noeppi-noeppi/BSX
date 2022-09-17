package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.AnyType;
import bsx.type.FloatType;
import bsx.type.IntegerType;

public class IntegerValue implements BsValue {
    
    public final int value;

    public IntegerValue(int value) {
        if ((value & 0x80000000) == 0) {
            // Positive number, fill with zeros
            this.value = value & 0x8000FFFF;
        } else {
            // Negative number, fill with ones
            this.value = value | 0x7FFF0000;
        }
    }

    @Override
    public BsType getType() {
        return IntegerType.INSTANCE;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || cls == Byte.class || cls == Short.class || cls == Integer.class || cls == Long.class || cls == Float.class || cls == Double.class;
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == Byte.class) {
            //noinspection unchecked
            return (T) Byte.valueOf((byte) this.value);
        } else if (cls == Short.class) {
            //noinspection unchecked
            return (T) Short.valueOf((short) this.value);
        } else if (cls == Integer.class) {
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