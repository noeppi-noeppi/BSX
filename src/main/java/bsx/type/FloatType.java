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

public enum FloatType implements NumericType {
    
    INSTANCE;

    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof BoolValue bv) {
            return new FloatingValue(bv.value ? 1 : 0);
        } else if (value instanceof IntegerValue iv) {
            return new FloatingValue(iv.value);
        } else if (value instanceof FloatingValue fv) {
            return fv;
        } else if (value instanceof StringValue sv) {
            Optional<String> str = sv.getPrintableString();
            if (str.isPresent()) {
                try {
                    return new FloatingValue(Double.parseDouble(str.get()));
                } catch (NumberFormatException e) {
                    //
                }
            }
            return FloatingValue.NaN;
        } else {
            return FloatingValue.NaN;
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
