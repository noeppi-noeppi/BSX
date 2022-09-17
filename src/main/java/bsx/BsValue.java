package bsx;

import bsx.type.AnyType;
import bsx.type.StringType;
import bsx.value.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface BsValue {
    
    BsType getType();
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || (BsValue.class.isAssignableFrom(cls) && cls.isAssignableFrom(this.getClass()));
    }
    
    default <T> T asJava(Class<T> cls) {
        if (cls == BsValue.class || (BsValue.class.isAssignableFrom(cls) && cls.isAssignableFrom(this.getClass()))) {
            //noinspection unchecked
            return (T) this;
        }
        throw new ClassCastException("Expected a value of type " + cls + ", got " + this + " (" + this.getType() + ")");
    }
    
    default boolean isOf(BsType type) {
        return type == AnyType.INSTANCE || Objects.equals(this.getType(), type);
    }
    
    static BsValue wrap(Object value) {
        if (value == null) {
            return NullValue.NULL;
        } else if (value instanceof BsValue v) {
            return v;
        } else if (value.getClass().isArray()) {
            List<BsValue> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(wrap(Array.get(value, i)));
            }
            return new ArrayValue(values);
        } else if (value instanceof List<?> list) {
            return new ArrayValue(list.stream().map(BsValue::wrap).toList());
        } else if (value instanceof String str) {
            return new StringValue(StringType.UTF256, str);
        } else if (value instanceof Boolean bool) {
            return bool ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (value instanceof Character chr) {
            return new StringValue(StringType.UTF256, Character.toString(chr));
        } else if (value instanceof Byte num) {
            // Bytes are wrapped unsigned
            return new IntegerValue(Byte.toUnsignedInt(num));
        } else if (value instanceof Short num) {
            return new IntegerValue(num);
        } else if (value instanceof Integer num) {
            return new IntegerValue(num);
        } else if (value instanceof Long num) {
            return new IntegerValue((int) (long) num);
        } else if (value instanceof Float num) {
            return new FloatingValue(num);
        } else if (value instanceof Double num) {
            return new FloatingValue(num);
        } else {
            return new ObjectValue(value);
        }
    }
}
