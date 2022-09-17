package bsx.type;

import bsx.BsValue;
import bsx.util.LooseEquality;
import bsx.value.FloatingValue;
import bsx.value.IntegerValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.OptionalDouble;

public enum FloatType implements NumericType {
    
    INSTANCE;

    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof IntegerValue iv) {
            return new FloatingValue(iv.value);
        } else if (value instanceof FloatingValue fv) {
            return fv;
        } else {
            OptionalDouble num = LooseEquality.getNum(value);
            if (num.isPresent()) {
                return new FloatingValue(num.getAsDouble());
            } else {
                return FloatingValue.NaN;
            }
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        return null;
    }
    
    @Override
    public String toString() {
        return "Float";
    }
}
