package bsx.type;

import bsx.BsType;
import bsx.BsValue;
import bsx.util.MethodUtil;
import bsx.value.ArrayValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;

public record ArrayType(BsType elementType) implements BsType {
    
    @Override
    public BsValue cast(BsValue value) {
        if (value.isOf(this)) {
            // Check that the element type matches to allow the cast
            return value;
        } else {
            throw new ClassCastException("Not an array: " + value);
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        if (instance && !special) {
            if (name.equals("__invoke") && args.size() == 2) {
                // get
                return MethodUtil.boundThis(MethodHandles.lookup().findVirtual(ArrayValue.class, "get", MethodType.methodType(BsValue.class, BsValue.class)), args.get(0));
            } else if (name.equals("__update") && args.size() == 3) {
                // set
                return MethodUtil.boundThis(MethodHandles.lookup().findVirtual(ArrayValue.class, "set", MethodType.methodType(BsValue.class, BsValue.class, BsValue.class)), args.get(0));
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType at) {
            return Objects.equals(this.elementType, at.elementType);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.elementType.hashCode() ^ 17;
    }

    @Override
    public String toString() {
        return this.elementType == AnyType.INSTANCE ? "Array" : this.elementType.toString() + "[]";
    }
}
