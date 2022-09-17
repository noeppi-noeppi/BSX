package bsx.compiler.jvm.statement;

import bsx.BsValue;
import bsx.compiler.ast.name.*;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.jvm.expression.InvokeExpressionCompiler;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.util.Bytecode;
import bsx.variable.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;
import java.util.stream.Stream;

public class AssignmentCompiler {
    
    public static InsnList compileAssignment(CompilerContext ctx, BlockScope scope, Labels labels, Assignment assignment) {
        if (assignment.target() instanceof ApplyCall call) {
            return compileUpdateCall(ctx, scope, labels, new UpdateCall(call.expr(), call.args(), assignment.newValue()));
        } else if (assignment.target() instanceof VariableName var) {
            InsnList instructions = new InsnList();
            
            int lvt;
            if (scope.hasVariable(var)) {
                lvt = scope.getVariable(var);
            } else {
                lvt = scope.newVariable(var);
                instructions.add(CommonCode.createVariableAt(var.name(), lvt));
            }
            
            instructions.add(new VarInsnNode(Opcodes.ALOAD, lvt));
            instructions.add(ExpressionCompiler.compile(ctx, scope, assignment.newValue()));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKEVIRTUAL, () -> Variable.class.getMethod("set", BsValue.class)));
            
            return instructions;
        } else if (assignment.target() instanceof Property property) {
            InsnList instructions = new InsnList();
            instructions.add(InvokeExpressionCompiler.compilePropertyAccess(ctx, scope, property, true, List.of(assignment.newValue())));
            instructions.add(new InsnNode(Opcodes.POP));
            return instructions;
        } else {
            throw new IllegalStateException("Invalid assignment target: " + assignment.target().getClass());
        }
    }
    
    public static InsnList compileUpdateCall(CompilerContext ctx, BlockScope scope, Labels labels, UpdateCall call) {
        InsnList instructions = new InsnList();
        instructions.add(ExpressionCompiler.compile(ctx, scope, call.expr()));
        instructions.add(new LdcInsnNode(""));
        instructions.add(CommonCode.makeExpressionArray(ctx, scope, Stream.concat(call.args().stream(), Stream.of(call.newValue())).toList()));
        instructions.add(CommonCode.makeInstanceCall(true));
        instructions.add(new InsnNode(Opcodes.POP));
        return instructions;
    }
}
