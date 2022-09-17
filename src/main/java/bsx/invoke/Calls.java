package bsx.invoke;

import bs.Predef;
import bsx.BSX;
import bsx.BsType;
import bsx.BsValue;
import bsx.resolution.Resolver;
import bsx.type.ClassType;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Calls {
    
    public static void doAnd(MethodHandle[] blocks) {
        Predef.doAnd(blocks);
    }
    
    public static void echo(BsValue value) {
        Predef.echo(value);
    }
    
    public static BsValue callPredef(String name, Object[] args, boolean special) {
        MethodHandle handle = BSX.resolve(ClassType.PREDEF, name, Arrays.stream(args).map(BsValue::wrap).toList(), special);
        try {
            return (BsValue) handle.invokeWithArguments(args);
        } catch (Throwable e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static BsValue callLocal(Class<?> localClass, @Nullable Object self, String name, Object[] args, boolean special) {
        MethodHandle handle = null;
        List<BsValue> argsWrapped = Arrays.stream(args).map(BsValue::wrap).toList();
        if (self != null) {
            // Resolve by type, not by value
            BsValue selfValue = BsValue.wrap(self);
            List<BsValue> argsWithInstance = Stream.concat(Stream.of(selfValue), argsWrapped.stream()).toList();
            try {
                handle = Resolver.resolve(localClass, name, argsWithInstance, true, special);
            } catch (ReflectiveOperationException e) {
                BSX.UNSAFE.throwException(e);
                throw new Error();
            }
            if (handle != null) handle = handle.bindTo(selfValue);
        }
        if (handle == null) {
            try {
                handle = Resolver.resolve(localClass, name, argsWrapped, false, special);
            } catch (ReflectiveOperationException e) {
                BSX.UNSAFE.throwException(e);
                throw new Error();
            }
        }
        if (handle == null) {
            handle = BSX.resolve(ClassType.PREDEF, name, Arrays.stream(args).map(BsValue::wrap).toList(), special);
        }
        try {
            return (BsValue) handle.invokeWithArguments(args);
        } catch (Throwable e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static BsValue callType(BsType type, String name, Object[] args, boolean special) {
        MethodHandle handle = BSX.resolve(type, name, Arrays.stream(args).map(BsValue::wrap).toList(), special);
        try {
            return (BsValue) handle.invokeWithArguments(args);
        } catch (Throwable e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static BsValue callValue(BsValue value, String name, Object[] args, boolean special) {
        MethodHandle handle = BSX.resolve(value, name, Arrays.stream(args).map(BsValue::wrap).toList(), special);
        MethodHandle bound = handle.bindTo(value);
        try {
            return (BsValue) bound.invokeWithArguments(args);
        } catch (Throwable e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
}
