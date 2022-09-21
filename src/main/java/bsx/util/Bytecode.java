package bsx.util;

import bsx.BSX;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Bytecode {
    
    public static Type getType(MethodType methodType) {
        Type returnType = Type.getType(methodType.returnType());
        Type[] argTypes = new Type[methodType.parameterCount()];
        for (int i = 0; i < methodType.parameterCount(); i++) {
            argTypes[i] = Type.getType(methodType.parameterType(i));
        }
        return Type.getMethodType(returnType, argTypes);
    }

    public static Type wrap(Type type) {
        return switch (type.getSort()) {
            case Type.VOID -> Type.getType(Void.class);
            case Type.BOOLEAN -> Type.getType(Boolean.class);
            case Type.BYTE -> Type.getType(Byte.class);
            case Type.CHAR -> Type.getType(Character.class);
            case Type.SHORT -> Type.getType(Short.class);
            case Type.INT -> Type.getType(Integer.class);
            case Type.LONG -> Type.getType(Long.class);
            case Type.FLOAT -> Type.getType(Float.class);
            case Type.DOUBLE -> Type.getType(Double.class);
            default -> type;
        };
    }

    public static Type unwrap(Type type) {
        if (type.getSort() != Type.OBJECT) return type;
        return switch (type.getInternalName()) {
            case "java/lang/Void" -> Type.VOID_TYPE;
            case "java/lang/Boolean" -> Type.BOOLEAN_TYPE;
            case "java/lang/Byte" -> Type.BYTE_TYPE;
            case "java/lang/Character" -> Type.CHAR_TYPE;
            case "java/lang/Short" -> Type.SHORT_TYPE;
            case "java/lang/Integer" -> Type.INT_TYPE;
            case "java/lang/Long" -> Type.LONG_TYPE;
            case "java/lang/Float" -> Type.FLOAT_TYPE;
            case "java/lang/Double" -> Type.DOUBLE_TYPE;
            default -> type;
        };
    }
    
    public static InsnList loadArgAsObject(int methodAccess, Type methodType, int arg) {
        int lvt = (methodAccess & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        Type[] argTypes = methodType.getArgumentTypes();
        for (int idx = 0; idx < arg; idx++) {
            Type argType = argTypes[idx];
            lvt += argType.getSize();
        }
        return loadAsObject(lvt, argTypes[arg]);
    }
    
    public static InsnList loadAsObject(int lvt, Type type) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), lvt));
        instructions.add(wrapValue(type));
        return instructions;
    }
    
    public static InsnList wrapValue(Type type) {
        InsnList instructions = new InsnList();
        switch (type.getSort()) {
            case Type.BOOLEAN -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Boolean.class.getMethod("valueOf", boolean.class)));
            case Type.BYTE -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Byte.class.getMethod("valueOf", byte.class)));
            case Type.CHAR -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Character.class.getMethod("valueOf", char.class)));
            case Type.SHORT -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Short.class.getMethod("valueOf", short.class)));
            case Type.INT -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Integer.class.getMethod("valueOf", int.class)));
            case Type.LONG -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Long.class.getMethod("valueOf", long.class)));
            case Type.FLOAT -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Float.class.getMethod("valueOf", float.class)));
            case Type.DOUBLE -> instructions.add(methodCall(Opcodes.INVOKESTATIC, () -> Double.class.getMethod("valueOf", double.class)));
        }
        return instructions;
    }
    
    public static InsnList unwrapValue(Type type) {
        InsnList instructions = new InsnList();
        switch (type.getSort()) {
            case Type.BOOLEAN -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Boolean.class.getMethod("booleanValue")));
            case Type.BYTE -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Byte.class.getMethod("byteValue")));
            case Type.CHAR -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Character.class.getMethod("charValue")));
            case Type.SHORT -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Short.class.getMethod("shortValue")));
            case Type.INT -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Integer.class.getMethod("intValue")));
            case Type.LONG -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Long.class.getMethod("longValue")));
            case Type.FLOAT -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Float.class.getMethod("floatValue")));
            case Type.DOUBLE -> instructions.add(methodCall(Opcodes.INVOKEVIRTUAL, () -> Double.class.getMethod("doubleValue")));
        }
        return instructions;
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
