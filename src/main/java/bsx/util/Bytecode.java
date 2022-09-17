package bsx.util;

import bsx.BSX;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Bytecode {
    
    public static final int API = Opcodes.ASM9;
    
    public static Type getType(MethodType methodType) {
        Type returnType = Type.getType(methodType.returnType());
        Type[] argTypes = new Type[methodType.parameterCount()];
        for (int i = 0; i < methodType.parameterCount(); i++) {
            argTypes[i] = Type.getType(methodType.parameterType(i));
        }
        return Type.getMethodType(returnType, argTypes);
    }
    
    public static MethodInsnNode methodCall(int opcode, ReflectiveSupplier<Method> method) {
        try {
            Method theMethod = method.get();
            return new MethodInsnNode(
                    opcode, Type.getType(theMethod.getDeclaringClass()).getInternalName(),
                    theMethod.getName(), Type.getType(theMethod).getDescriptor(),
                    theMethod.getDeclaringClass().isInterface()
            );
        } catch (ReflectiveOperationException e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static Handle fieldHandle(int tag, ReflectiveSupplier<Field> field) {
        try {
            Field theField = field.get();
            return new Handle(
                    tag, Type.getType(theField.getDeclaringClass()).getInternalName(),
                    theField.getName(), Type.getType(theField.getType()).getDescriptor(),
                    false
            );
        } catch (ReflectiveOperationException e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }

    public static Handle constructorHandle(ReflectiveSupplier<Constructor<?>> ctor) {
        try {
            Constructor<?> constructor = ctor.get();
            return new Handle(
                    Opcodes.H_NEWINVOKESPECIAL, Type.getType(constructor.getDeclaringClass()).getInternalName(),
                    "<init>", Type.getType(constructor).getDescriptor(), false
            );
        } catch (ReflectiveOperationException e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    public static Handle methodHandle(int tag, ReflectiveSupplier<Method> method) {
        try {
            Method theMethod = method.get();
            return new Handle(
                    tag, Type.getType(theMethod.getDeclaringClass()).getInternalName(),
                    theMethod.getName(), Type.getType(theMethod).getDescriptor(),
                    theMethod.getDeclaringClass().isInterface()
            );
        } catch (ReflectiveOperationException e) {
            BSX.UNSAFE.throwException(e);
            throw new Error();
        }
    }
    
    @FunctionalInterface
    public interface ReflectiveSupplier<T> {
        
        T get() throws ReflectiveOperationException;
    }
}
