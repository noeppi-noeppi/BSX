package bsx.type;

import bsx.BsType;
import bsx.BsValue;
import bsx.value.*;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;

public enum BoolType implements BsType {
    
    INSTANCE;

    public boolean isTrue(BsValue value) {
        if (this.cast(value) instanceof BoolValue bv) {
            return bv.value;
        } else {
            return false;
        }
    }
    
    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof BoolValue bv) {
            return bv;
        } else if (value instanceof IntegerValue iv) {
            return iv.value != 0 ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (value instanceof FloatingValue fv) {
            return fv.value != 0 ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (value instanceof StringValue sv) {
            return sv.getPrintableString().filter(str -> str.isEmpty() || "0".equals(str)).isPresent() ? BoolValue.FALSE : BoolValue.TRUE;
        } else if (value instanceof ArrayValue av) {
            return av.length() > 0 ? BoolValue.TRUE : BoolValue.FALSE;
        } else {
            return BoolValue.TRUE;
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        return null;
    }

    @Override
    public String toString() {
        return "Boolean";
    }
}
