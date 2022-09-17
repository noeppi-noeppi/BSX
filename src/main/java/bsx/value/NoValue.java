package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.util.MethodUtil;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.NoSuchElementException;

public enum NoValue implements BsValue, BsType {
    
    INSTANCE;

    @Override
    public BsType getType() {
        return this;
    }

    @Override
    public BsValue cast(BsValue value) {
        return this;
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        MethodHandle handle = MethodHandles.lookup().findStatic(NoValue.class, "call", MethodType.methodType(void.class, String.class, boolean.class));
        MethodHandle withExtra = MethodHandles.insertArguments(handle, 0, name, instance);
        return MethodUtil.resolveSimpleCall(withExtra, args.size());
    }
    
    private static void call(String name, boolean instance) {
        throw new NoSuchElementException("void" + (instance ? "->" : "::") + name);
    }
    
    @Override
    public String toString() {
        return "void";
    }
}
