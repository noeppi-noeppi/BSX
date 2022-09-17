package bsx.compiler.jvm.statement;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;
import bsx.compiler.ast.lang.*;
import bsx.compiler.ast.name.Assignment;
import bsx.compiler.ast.name.UpdateCall;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.util.List;

// Assignment, UpdateCall
public class StatementCompiler {
    
    public static InsnList compile(CompilerContext ctx, BlockScope scope, List<Statement> statements) {
        return compile(ctx, scope, statements, new Labels());
    }
    
    public static InsnList compile(CompilerContext ctx, BlockScope scope, List<Statement> statements, Labels specialReachLabels) {
        Labels labels = new Labels(specialReachLabels);
        for (Statement stmt : statements) {
            if (stmt instanceof LabelledStatement ls) {
                labels.addLabel(ls.label());
            }
        }
        labels.freeze();
        
        InsnList instructions = new InsnList();
        for (Statement stmt : statements) {
            if (stmt instanceof LabelledStatement ls) {
                instructions.add(labels.getLabel(ls.label()));
                instructions.add(compileStatement(ctx, scope, labels, ls.stmt()));
            } else {
                instructions.add(compileStatement(ctx, scope, labels, stmt));
            }
        }
        return instructions;
    }
    
    private static InsnList compileStatement(CompilerContext ctx, BlockScope scope, Labels labels, Statement statement) {
        if (statement instanceof Expression expression) {
            InsnList instructions = new InsnList();
            instructions.add(ExpressionCompiler.compile(ctx, scope, expression));
            instructions.add(new InsnNode(Opcodes.POP));
            return instructions;
        } else if (statement instanceof NoOp) {
            return new InsnList();
        } else if (statement instanceof Echo echo) {
            return SpecialCallStatementCompiler.compileEcho(ctx, scope, labels, echo);
        } else if (statement instanceof Goto goto_) {
            return SpecialCallStatementCompiler.compileGoto(ctx, scope, labels, goto_);
        } else if (statement instanceof Return return_) {
            return SpecialCallStatementCompiler.compileReturn(ctx, scope, labels, return_);
        } else if (statement instanceof DoAnd doAnd) {
            return SpecialCallStatementCompiler.compileDoAnd(ctx, scope, labels, doAnd);
        } else if (statement instanceof Deletion del) {
            return SpecialCallStatementCompiler.compileDelete(ctx, scope, labels, del);
        } else if (statement instanceof BranchCondition condition) {
            return ConditionCompiler.compileCondition(ctx, scope, labels, condition);
        } else if (statement instanceof Assignment assignment) {
            return AssignmentCompiler.compileAssignment(ctx, scope, labels, assignment);
        } else if (statement instanceof UpdateCall call) {
            return AssignmentCompiler.compileUpdateCall(ctx, scope, labels, call);
        } else {
            throw new IllegalArgumentException("Can't compile statement of type " + statement.getClass());
        }
    }
}
