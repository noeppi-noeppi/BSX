package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.AnyType;
import bsx.type.ClassType;

import java.util.Objects;

public class ObjectValue implements BsValue {
    
    public final Object value;
    private final BsType type;

    public ObjectValue(Object value) {
        this.value = Objects.requireNonNull(value);
        this.type = new ClassType(value.getClass());
    }

    @Override
    public BsType getType() {
        return this.type;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || cls.isAssignableFrom(this.value.getClass());
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == BsValue.class) {
            //noinspection unchecked
            return (T) this;
        } else if (cls.isAssignableFrom(this.value.getClass())) {
            //noinspection unchecked
            return (T) this.value;
        } else {
            return BsValue.super.asJava(cls);
        }
    }

    @Override
    public boolean isOf(BsType type) {
        if (type == AnyType.INSTANCE) {
            return true;
        } else if (type instanceof ClassType ct) {
            return ct.cls().isAssignableFrom(this.value.getClass());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObjectValue ov) {
            return Objects.equals(this.value, ov.value);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
