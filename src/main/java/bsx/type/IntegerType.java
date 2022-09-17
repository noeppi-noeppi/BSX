package bsx.type;

import bsx.BsValue;
import bsx.value.BoolValue;
import bsx.value.FloatingValue;
import bsx.value.IntegerValue;
import bsx.value.StringValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;

public enum IntegerType implements NumericType {
    
    INSTANCE;
    
    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof BoolValue bv) {
            return new IntegerValue(bv.value ? 1 : 0);
        } else if (value instanceof IntegerValue iv) {
            return iv;
        } else if (value instanceof FloatingValue fv) {
            return new IntegerValue(Double.valueOf(fv.value).intValue());
        } else if (value instanceof StringValue sv) {
            Optional<String> str = sv.getPrintableString();
            if (str.isPresent()) {
                try {
                    return new IntegerValue(Integer.parseInt(str.get()));
                } catch (NumberFormatException e) {
                    //
                }
            }
            throw new ClassCastException(value + " cannot be cast to " + this);
        } else {
            throw new ClassCastException(value + " cannot be cast to " + this);
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
