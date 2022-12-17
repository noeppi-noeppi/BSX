package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.FloatType;

public class FloatingValue implements BsValue {
    
    public static final FloatingValue PIE = new FloatingValue(Math.PI, true);
    public static final FloatingValue NaN = new FloatingValue(Double.NaN);
    
    public final double value;
    public final boolean isPieEmoji; // Pi emoji behaves as Math.PI but equals 22/7 with non-strict equal

    private FloatingValue(double value, boolean isPieEmoji) {
        this.value = value;
        this.isPieEmoji = isPieEmoji;
    }
    
    public FloatingValue(double value) {
        this.value = value;
        this.isPieEmoji = false;
    }

    @Override
    public BsType getType() {
        return FloatType.INSTANCE;
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
        } else if (cls == Integer.class) {
            //noinspection unchecked
            return (T) Integer.valueOf((int) this.value);
        } else if (cls == Long.class) {
            //noinspection unchecked
            return (T) Long.valueOf((long) this.value);
        } else if (cls == Float.class) {
            //noinspection unchecked
            return (T) Float.valueOf((float) this.value);
        } else if (cls == Double.class || cls == Object.class) {
            //noinspection unchecked
            return (T) Double.valueOf(this.value);
        } else {
            return BsValue.super.asJava(cls);
        }
    }
    
    @Override
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FloatingValue fv) {
            return this.value == fv.value;
        } else if (obj instanceof IntegerValue iv) {
            return this.value == iv.value;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Double.toString(this.value);
    }
}
