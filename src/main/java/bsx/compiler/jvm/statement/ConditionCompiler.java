package bsx.compiler.jvm.statement;

import bsx.BsValue;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Line;
import bsx.compiler.ast.lang.BranchCondition;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Values;
import bsx.util.Bytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;

public class ConditionCompiler {
    
    public static InsnList compileCondition(CompilerContext ctx, BlockScope scope, Labels labels, BranchCondition condition) {
        if (condition.ifTrue().isEmpty()) {
            return compileSimpleCondition(ctx, scope, labels, condition.conditionLineNumber(), condition.condition(), condition.ifFalse());
        } else {
            return compileMultiCondition(ctx, scope, labels, condition.conditionLineNumber(), condition.condition(), condition.ifTrue(), condition.ifFalse());
        }
    }
    
    private static InsnList compileSimpleCondition(CompilerContext ctx, BlockScope scope, Labels labels, int conditionLineNumber, Expression expr, List<Line> ifFalse) {
        LabelNode end = new LabelNode();
        end.getLabel();
        
        InsnList instructions = new InsnList();
        
        instructions.add(CommonCode.lineNumber(conditionLineNumber));
        instructions.add(ExpressionCompiler.compile(ctx, scope, expr));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("isTrue", BsValue.class)));
        instructions.add(new JumpInsnNode(Opcodes.IFNE, end));

        BlockScope falseScope = BlockScope.makeSameMethodInnerScope(scope);
        instructions.add(StatementCompiler.compile(ctx, falseScope, ifFalse, labels));
        instructions.add(falseScope.end());
        
        instructions.add(end);
        
        return instructions;
    }
    
    private static InsnList compileMultiCondition(CompilerContext ctx, BlockScope scope, Labels labels, int conditionLineNumber, Expression expr, List<Line> ifTrue, List<Line> ifFalse) {
        LabelNode else_ = new LabelNode();
        else_.getLabel();
        
        LabelNode end = new LabelNode();
        end.getLabel();
        
        InsnList instructions = new InsnList();
        
        instructions.add(CommonCode.lineNumber(conditionLineNumber));
        instructions.add(ExpressionCompiler.compile(ctx, scope, expr));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("isTrue", BsValue.class)));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, else_));
        
        BlockScope trueScope = BlockScope.makeSameMethodInnerScope(scope);
        instructions.add(StatementCompiler.compile(ctx, trueScope, ifTrue, labels));
        instructions.add(trueScope.end());
        
        instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
        instructions.add(else_);
        
        BlockScope falseScope = BlockScope.makeSameMethodInnerScope(scope);
        instructions.add(StatementCompiler.compile(ctx, falseScope, ifFalse, labels));
        instructions.add(falseScope.end());
        
        instructions.add(end);
        
        return instructions;
    }
}
