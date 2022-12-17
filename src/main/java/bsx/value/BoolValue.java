package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.BoolType;

public enum BoolValue implements BsValue {
    
    TRUE(true),
    FALSE(false);
    
    public final boolean value;

    BoolValue(boolean value) {
        this.value = value;
    }

    @Override
    public BsType getType() {
        return BoolType.INSTANCE;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || cls == Boolean.class || cls == Object.class;
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        //noinspection unchecked
        return (cls == Boolean.class || cls == Object.class) ? (T) Boolean.valueOf(this.value) : BsValue.super.asJava(cls);
    }

    @Override
    public String toString() {
        return Boolean.toString(this.value);
    }

    public static BoolValue of(boolean value) {
        return value ? TRUE : FALSE;
    }
}
