package bsx.type;

import bsx.BsType;
import bsx.BsValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;

public interface NumericType extends BsType {

    @Nullable
    @Override
    default MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        return null;
    }
}
