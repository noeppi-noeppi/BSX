package bsx.compiler.jvm.optimize;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.util.List;

public class BytecodeOptimizer {
    
    public static void optimize(List<ClassNode> classes) {
        for (ClassNode node : classes) {
            optimize(node);
        }
    }
    
    public static void optimize(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.instructions != null) {
                InsnList instructions = method.instructions;
                for (AbstractInsnNode instruction : instructions) {
                    AbstractInsnNode newNode = optimizeInstruction(instruction);
                    if (newNode != null) instructions.set(instruction, newNode);
                }
            }
        }
    }
    
    @Nullable
    private static AbstractInsnNode optimizeInstruction(AbstractInsnNode instruction) {
        if (instruction instanceof LdcInsnNode ldc) {
            if (ldc.cst instanceof Integer i) {
                return getIntLoadInstruction(i);
            } else if (ldc.cst instanceof Float f) {
                if (f == 0) return new InsnNode(Opcodes.FCONST_0);
                if (f == 1) return new InsnNode(Opcodes.FCONST_1);
                if (f == 2) return new InsnNode(Opcodes.FCONST_2);
            } else if (ldc.cst instanceof Double d) {
                if (d == 0) return new InsnNode(Opcodes.DCONST_0);
                if (d == 1) return new InsnNode(Opcodes.DCONST_1);
            }
        } else if (instruction instanceof IntInsnNode ii) {
            if (ii.getOpcode() == Opcodes.BIPUSH || ii.getOpcode() == Opcodes.SIPUSH) {
                return getIntLoadInstruction(ii.operand);
            }
        }
        return null;
    }
    
    private static AbstractInsnNode getIntLoadInstruction(int value) {
        switch (value) {
            case -1: return new InsnNode(Opcodes.ICONST_M1);
            case 0: return new InsnNode(Opcodes.ICONST_0);
            case 1: return new InsnNode(Opcodes.ICONST_1);
            case 2: return new InsnNode(Opcodes.ICONST_2);
            case 3: return new InsnNode(Opcodes.ICONST_3);
            case 4: return new InsnNode(Opcodes.ICONST_4);
            case 5: return new InsnNode(Opcodes.ICONST_5);
        }
        if (value >= Byte.MIN_VALUE && value < Byte.MAX_VALUE) return new IntInsnNode(Opcodes.BIPUSH, value & 0xFF);
        if (value >= Short.MIN_VALUE && value < Short.MAX_VALUE) return new IntInsnNode(Opcodes.SIPUSH, value & 0xFFFF);
        return new LdcInsnNode(value);
    }
}
