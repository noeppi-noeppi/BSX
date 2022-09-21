package bsx.invoke;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.*;
import bsx.util.ClassUtil;
import bsx.value.BoolValue;
import bsx.value.NoValue;
import bsx.value.NullValue;

import java.lang.invoke.MethodHandles;

public class Types {
    
    public static BsValue checkType(BsValue value, BsType type) {
        if (!value.isOf(type)) {
            throw new ClassCastException("Expected value of type " + type + ", got " + value.getType());
        }
        return value;
    }
    
    public static BsValue isInstance(BsValue value, BsType type) {
        return BoolValue.of(value.isOf(type));
    }
    
    public static BsValue castType(BsValue value, BsType type) {
        return type.cast(value);
    }
    
    public static BsType primitiveType(MethodHandles.Lookup lookup, String name, Class<?> type, int id) {
        return switch (id) {
            case 0 -> AnyType.INSTANCE;
            case 1 -> NoValue.INSTANCE;
            case 10 -> NullValue.NULL;
            case 11 -> NullValue.NOTHING;
            case 12 -> NullValue.UNDEFINED;
            case 13 -> NullValue.NADA;
            case 14 -> NullValue.EMPTY;
            case 20 -> BoolType.INSTANCE;
            case 21 -> IntegerType.INSTANCE;
            case 22 -> FloatType.INSTANCE;
            case 30 -> StringType.ASCII;
            case 31 -> StringType.ANSI;
            case 32 -> StringType.DBCS;
            case 33 -> StringType.EBCDIC;
            case 34 -> StringType.UTF256;
            default -> throw new IncompatibleClassChangeError();
        };
    }
    
    public static BsType arrayType(MethodHandles.Lookup lookup, String name, Class<?> type, BsType elementType) {
        return new ArrayType(elementType);
    }
    
    public static BsType classType(MethodHandles.Lookup lookup, String name, Class<?> type, Class<?> typeClass) {
        if (typeClass.isPrimitive() || typeClass.isArray() || ClassUtil.unboxed(type).isPrimitive() || typeClass == String.class) {
            throw new IncompatibleClassChangeError();
        } else {
            return new ClassType(typeClass);
        }
    }
}
