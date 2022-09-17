package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.AnyType;
import bsx.type.ArrayType;
import bsx.util.ValueHelper;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ArrayValue implements BsValue {
    
    private static final BsType TYPE = new ArrayType(AnyType.INSTANCE);
    
    private final List<BsValue> values;

    public ArrayValue(List<BsValue> values) {
        this.values = new ArrayList<>(values);
    }

    public int length() {
        return this.values.size();
    }
    
    public BsValue get(BsValue idx) {
        OptionalInt theIdx = ValueHelper.getIntegral(idx, -this.length(), -1);
        if (theIdx.isPresent()) {
            return this.values.get(-theIdx.getAsInt() - 1);
        } else {
            throw new IndexOutOfBoundsException("Array not defined at " + idx + ", length=" + this.length());
        }
    }
    
    public BsValue set(BsValue idx, BsValue value) {
        OptionalInt theIdx = ValueHelper.getIntegral(idx, -this.length(), -1);
        if (theIdx.isPresent()) {
            return this.values.set(-theIdx.getAsInt() - 1, value);
        } else {
            throw new IndexOutOfBoundsException("Array not defined at " + idx + ", length=" + this.length());
        }
    }
    
    public List<BsValue> values() {
        return Collections.unmodifiableList(this.values);
    }
    
    @Override
    public BsType getType() {
        return TYPE;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        if (cls == BsValue.class || cls == List.class) {
            return true;
        } else if (cls.isArray()) {
            for (BsValue elem : this.values) {
                if (!elem.matchesJava(cls.componentType())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == BsValue.class) {
            //noinspection unchecked
            return (T) this;
        } else if (cls == List.class) {
            //noinspection unchecked
            return (T) this.values(); // Unmodifiable view
        } else if (cls.isArray()) {
            Object[] array = (Object[]) Array.newInstance(cls.componentType(), this.values.size());
            for (int i = 0; i < this.values.size(); i++) {
                array[i] = this.values.get(i).asJava(cls.componentType());
            }
            //noinspection unchecked
            return (T) array;
        } else {
            return BsValue.super.asJava(cls);
        }
    }

    @Override
    public boolean isOf(BsType type) {
        if (type == AnyType.INSTANCE) {
            return true;
        } else if (type instanceof ArrayType at) {
            if (at.elementType() != AnyType.INSTANCE) {
                for (BsValue elem : this.values) {
                    if (!elem.isOf(at.elementType())) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.values.toArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayValue av && this.values.size() == av.values.size()) {
            for (int i = 0; i < this.values.size(); i++) {
                if (!Objects.equals(this.values.get(i), av.values.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.values.isEmpty()) return "[]";
        return "[ " + this.values.stream().map(BsValue::toString).collect(Collectors.joining(", ")) + " ]";
    }
}
