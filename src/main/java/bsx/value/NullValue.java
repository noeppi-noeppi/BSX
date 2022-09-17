package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.util.MethodUtil;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public enum NullValue implements BsValue, BsType {
    
    NULL("null"),
    NOTHING("Nothing"),
    UNDEFINED("undefined"),
    NADA("nada"),
    EMPTY("Empty");
    
    public final String literal;

    NullValue(String literal) {
        this.literal = literal;
    }
    
    @Override
    public BsType getType() {
        return this;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return !cls.isPrimitive() && cls != void.class;
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == BsValue.class) {
            //noinspection unchecked
            return (T) this;
        } else {
            return null;
        }
    }

    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof NullValue) {
            return this;
        } else {
            throw new ClassCastException(value + " is not an instance of " + this);
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        MethodHandle handle = MethodHandles.lookup().findVirtual(NullValue.class, "dereference", MethodType.methodType(void.class, String.class, boolean.class));
        MethodHandle withExtra = MethodHandles.insertArguments(handle, 0, this, name, instance);
        return MethodUtil.resolveSimpleCall(withExtra, args.size());
    }
    
    private void dereference(String name, boolean instance) {
        throw new NullPointerException(this.literal + " reference (" + this.literal + (instance ? "->" : "::") + name + ")");
    }
    
    @Override
    public String toString() {
        return this.literal;
    }
}
