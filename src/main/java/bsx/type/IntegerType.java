package bsx.type;

import bsx.BsValue;
import bsx.util.LooseEquality;
import bsx.value.FloatingValue;
import bsx.value.IntegerValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.OptionalDouble;

public enum IntegerType implements NumericType {
    
    INSTANCE;
    
    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof IntegerValue iv) {
            return iv;
        } else if (value instanceof FloatingValue fv) {
            return new IntegerValue((int) fv.value);
        } else {
            OptionalDouble num = LooseEquality.getNum(value);
            if (num.isPresent()) {
                return new IntegerValue((int) num.getAsDouble());
            } else {
                throw new ClassCastException(value + " cannot be cast to " + this);
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
        return "Integer";
    }
}
