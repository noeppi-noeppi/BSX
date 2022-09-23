package bsx.compiler.jvm.override;

import bsx.util.Bytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EqualsFactory {
    
    @Nullable
    public static MethodNode makeEquals(ClassNode node) {
        if ((node.access & Opcodes.ACC_ABSTRACT) != 0) return null;
        
        for (MethodNode method : node.methods) {
            if (method.name.equals("equals") && method.desc.equals("(Ljava/lang/Object;)Z")) {
                return null;
            }
        }
        
        List<FieldNode> fields = new ArrayList<>();
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_STATIC) == 0 && (field.access & Opcodes.ACC_TRANSIENT) == 0) {
                fields.add(field);
            }
        }
        
        MethodNode method = new MethodNode();
        method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_MANDATED;
        method.name = "equals";
        method.desc = "(Ljava/lang/Object;)Z";
        
        method.parameters = new ArrayList<>();
        method.parameters.add(new ParameterNode("that", 0));

        // Check for reference equality
        LabelNode notReferenceEqual = new LabelNode();
        notReferenceEqual.getLabel();
        
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPNE, notReferenceEqual));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.add(notReferenceEqual);
        
        // Check for null
        LabelNode nonNull = new LabelNode();
        nonNull.getLabel();
        
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, nonNull));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.add(nonNull);
        
        // Check that methods share the same class
        LabelNode sameClass = new LabelNode();
        sameClass.getLabel();
        
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(Bytecode.methodCall(Opcodes.INVOKEVIRTUAL, () -> Object.class.getMethod("getClass")));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(Bytecode.methodCall(Opcodes.INVOKEVIRTUAL, () -> Object.class.getMethod("getClass")));
        method.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, sameClass));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.add(sameClass);
        
        String superName = node.superName == null ? "java/lang/Object" : node.superName;
        boolean insertSuperCall = fields.isEmpty() || !"java/lang/Object".equals(superName);
        if (insertSuperCall) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, superName, "equals", "(Ljava/lang/Object;)Z", false));
            if (fields.isEmpty()) {
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
            } else {
                LabelNode superEqual = new LabelNode();
                superEqual.getLabel();
                method.instructions.add(new JumpInsnNode(Opcodes.IFNE, superEqual));
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
                method.instructions.add(superEqual);
            }
        }
        
        if (!fields.isEmpty()) {
            method.instructions.add(compareFields(node, fields));
        }
        
        return method;
    }
    
    private static InsnList compareFields(ClassNode node, List<FieldNode> fields) {
        InsnList instructions = new InsnList();
        
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, node.name));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));
        
        for (FieldNode field : fields) {
            instructions.add(compareField(node, field));
        }
        
        instructions.add(new InsnNode(Opcodes.ICONST_1));
        instructions.add(new InsnNode(Opcodes.IRETURN));
        
        return instructions;
    }
    
    private static InsnList compareField(ClassNode node, FieldNode field) {
        InsnList instructions = new InsnList();
        
        LabelNode isEqual = new LabelNode();
        isEqual.getLabel();

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, field.name, field.desc));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, field.name, field.desc));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Objects.class.getMethod("equals", Object.class, Object.class)));
        instructions.add(new JumpInsnNode(Opcodes.IFNE, isEqual));
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new InsnNode(Opcodes.IRETURN));
        instructions.add(isEqual);
        
        return instructions;
    }
}
