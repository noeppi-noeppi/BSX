package bsx.type;

import bsx.BsType;
import bsx.BsValue;
import bsx.util.MethodUtil;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public enum AnyType implements BsType {
    
    INSTANCE;
    
    @Override
    public BsValue cast(BsValue value) {
        return value;
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        MethodHandle handle = MethodHandles.lookup().findStatic(AnyType.class, "call", MethodType.methodType(void.class));
        return MethodUtil.resolveSimpleCall(handle, args.size());
    }
    
    private static void call() {
        throw new IllegalStateException("Unknown resolution type (any)");
    }
    
    @Override
    public String toString() {
        return "Any";
    }
}
