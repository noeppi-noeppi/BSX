package bsx.compiler.lvt;

import bsx.compiler.ast.name.VariableName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class EmptyScope extends BlockScope {

    public static final EmptyScope INSTANCE = new EmptyScope();

    private EmptyScope() {
        super(false, 0);
    }

    @Override
    public int newVariable(VariableName var) {
        throw new IllegalArgumentException("Can't create variables here");
    }

    @Override
    public InsnList createVarArray() {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(Object.class).getInternalName()));
        return instructions;
    }
}
