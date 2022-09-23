package bsx.compiler.jvm.override;

import bsx.BsValue;
import bsx.invoke.Values;
import bsx.resolution.NoLookup;
import bsx.util.Bytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class BridgeFactory {
    
    public static MethodNode makeBridgeFor(String owner, MethodNode original, Type methodType) {
        if ((original.access & Opcodes.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("static bridge");
        }
        if (Type.getMethodType(original.desc).getArgumentTypes().length != methodType.getArgumentTypes().length) {
            throw new IllegalArgumentException("arg length mismatch");
        }
        MethodNode node = new MethodNode();
        node.access = original.access | Opcodes.ACC_BRIDGE;
        node.name = original.name;
        node.desc = methodType.getDescriptor();
        
        if (original.parameters != null) {
            node.parameters = new ArrayList<>();
            for (ParameterNode arg : original.parameters) {
                node.parameters.add(new ParameterNode(arg.name, 0));
            }
        }

        node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(new AnnotationNode(Type.getType(Override.class).getDescriptor()));
        // Bridges should not be looked up, should always find the actual method
        node.visibleAnnotations.add(new AnnotationNode(Type.getType(NoLookup.class).getDescriptor()));
        
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int argLen = methodType.getArgumentTypes().length;
        for (int i = 0; i < argLen; i++) {
            node.instructions.add(Bytecode.loadArgAsObject(node.access, methodType, i));
            node.instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("wrapValue", Object.class)));
        }
        
        node.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, original.name, original.desc, false));
        
        if (node.name.equals("toString") && node.desc.equals("()Ljava/lang/String;")) {
            // Special case: Call toString on object
            node.instructions.add(Bytecode.methodCall(Opcodes.INVOKEVIRTUAL, () -> Object.class.getMethod("toString")));
            node.instructions.add(new InsnNode(Opcodes.ARETURN));
        } else if (methodType.getReturnType().getSort() == Type.VOID) {
            node.instructions.add(new InsnNode(Opcodes.POP));
            node.instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            node.instructions.add(new LdcInsnNode(Bytecode.wrap(methodType.getReturnType())));
            node.instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("unwrapValue", BsValue.class, Class.class)));
            node.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Bytecode.wrap(methodType.getReturnType()).getInternalName()));
            node.instructions.add(Bytecode.unwrapValue(methodType.getReturnType()));
            node.instructions.add(new InsnNode(methodType.getReturnType().getOpcode(Opcodes.IRETURN)));
        }
        
        return node;
    }
}
