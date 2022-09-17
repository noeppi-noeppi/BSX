package bsx;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;

public interface BsType {
    
    BsValue cast(BsValue value);
    
    // empty name, special=false is just the value with parens
    // empty name, special=true is value(...) = ...
    
    @Nullable
    MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException;
}
