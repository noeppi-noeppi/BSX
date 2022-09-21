package bsx;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;

public interface BsType {
    
    BsValue cast(BsValue value);
    
    // __invoke, special=false is just the value with parens
    // __update, special=false is value(...) = ...
    
    @Nullable
    MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException;
}
