package bsx.compiler.lvt;

import bsx.BsValue;
import bsx.compiler.ast.name.VariableName;
import bsx.invoke.Language;
import bsx.util.Bytecode;
import bsx.variable.Variable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.stream.Stream;

public sealed class BlockScope permits EmptyScope {
    
    public final int offset;
    private final List<String> parentVariables;
    private final List<String> ownVariables;
    private final Set<String> variableNames;
    private final Set<String> deletedVariables;
    
    public BlockScope(boolean instance, int argOffset) {
        this.offset = (instance ? 1 : 0) + argOffset;
        this.parentVariables = List.of();
        this.ownVariables = new ArrayList<>();
        this.variableNames = new HashSet<>();
        this.deletedVariables = new HashSet<>();
    }
    
    public BlockScope(boolean instance, int argOffset, BlockScope parent) {
        this.offset = (instance ? 1 : 0) + argOffset;
        this.parentVariables = Stream.concat(parent.parentVariables.stream(), parent.ownVariables.stream()).toList();
        this.ownVariables = new ArrayList<>();
        this.variableNames = new HashSet<>(parent.variableNames);
        this.deletedVariables = new HashSet<>(parent.deletedVariables);
    }
    
    private BlockScope(BlockScope parent) {
        this.offset = parent.offset;
        this.parentVariables = Stream.concat(parent.parentVariables.stream(), parent.ownVariables.stream()).toList();
        this.ownVariables = new ArrayList<>();
        this.variableNames = new HashSet<>(parent.variableNames);
        this.deletedVariables = new HashSet<>(parent.deletedVariables);
    }
    
    public BlockScope(Scope parent) {
        this.offset = parent.offset();
        this.parentVariables = List.of();
        this.ownVariables = new ArrayList<>(parent.availableVariables());
        this.variableNames = new HashSet<>(this.ownVariables);
        this.deletedVariables = new HashSet<>(parent.deletedVariables());
    }
    
    public static BlockScope makeSameMethodInnerScope(BlockScope parent) {
        return new BlockScope(parent);
    }
    
    public boolean hasVariable(VariableName var) {
        return this.variableNames.contains(var.name());
    }
    
    public List<String> allVariables() {
        return Stream.concat(this.parentVariables.stream(), this.ownVariables.stream()).toList();
    }
    
    public Set<String> deletedVariables() {
        return Set.copyOf(this.deletedVariables);
    }
    
    public int newVariable(VariableName var) {
        if (this.variableNames.contains(var.name())) {
            throw new IllegalStateException("Variable €" + var.name() + " already defined in the scope.");
        } else {
            this.variableNames.add(var.name());
            this.ownVariables.add(var.name());
            return this.offset + this.parentVariables.size() + this.ownVariables.indexOf(var.name());
        }
    }
    
    public int getVariable(VariableName var) {
        if (this.deletedVariables.contains(var.name())) {
            throw new IllegalStateException("Variable €" + var.name() + " has already been deleted.");
        } else if (this.parentVariables.contains(var.name())) {
            return this.offset + this.parentVariables.indexOf(var.name());
        } else if (this.ownVariables.contains(var.name())) {
            return this.offset + this.parentVariables.size() + this.ownVariables.indexOf(var.name());
        } else {
            throw new IllegalStateException("Unbound variable €" + var.name() + ".");
        }
    }
    
    public void deleteVariable(VariableName var) {
        if (this.deletedVariables.contains(var.name())) {
            throw new IllegalStateException("Variable €" + var.name() + " has already been deleted.");
        } else if (this.parentVariables.contains(var.name())) {
            throw new IllegalStateException("Variable €" + var.name() + " must be deleted in the same scope it has been created.");
        } else if (this.ownVariables.contains(var.name())) {
            this.deletedVariables.add(var.name());
        } else {
            throw new IllegalStateException("Unbound variable €" + var.name() + ".");
        }
    }
    
    public void checkDeleted() {
        List<String> undeleted = this.ownVariables.stream()
                .filter(name -> !this.deletedVariables.contains(name))
                .map(name -> "€" + name)
                .toList();
        if (!undeleted.isEmpty()) {
            throw new IllegalStateException("Variables must be deleted: " + String.join(", ", undeleted));
        }
    }
    
    public MethodType innerBlockType() {
        Class<?>[] args = new Class<?>[this.parentVariables.size() + this.ownVariables.size()];
        Arrays.fill(args, Variable.class);
        return MethodType.methodType(BsValue.class, args);
    }
    
    // Array type is always Object
    public InsnList createVarArray() {
        int totalVars = this.parentVariables.size() + this.ownVariables.size();
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(totalVars));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(Object.class).getInternalName()));
        for (int i = 0; i < totalVars; i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new LdcInsnNode(i));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, this.offset + i));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        return instructions;
    }
    
    // Puts a new MethodHandle on the stack that can serve as block (callable without args, variables inserted)
    // blockImpl must have an INVOKESTATIC tag if not called from an instance method.
    public InsnList makeBlock(Handle blockImpl) {
        boolean needsThisArg = blockImpl.getTag() != Opcodes.H_INVOKESTATIC;
        int totalVars = this.parentVariables.size() + this.ownVariables.size();
        
        InsnList instructions = new InsnList();
        if (needsThisArg) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        for (int i = 0; i < totalVars; i++) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, this.offset + i));
        }

        MethodType methodType = this.innerBlockType();
        if (needsThisArg) {
            methodType = methodType.insertParameterTypes(0, Object.class);
        }
        methodType = methodType.changeReturnType(MethodHandle.class);
        
        instructions.add(new InvokeDynamicInsnNode(
                "block", Bytecode.getType(methodType).getDescriptor(),
                Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Language.class.getMethod("makeBlock", MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class)),
                blockImpl
        ));
        return instructions;
    }
}
