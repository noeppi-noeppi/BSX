package bsx.compiler.jvm.expression;

import bsx.BsType;
import bsx.BsValue;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.lang.ObjectCreation;
import bsx.compiler.ast.literal.Literal;
import bsx.compiler.ast.name.ApplyCall;
import bsx.compiler.ast.name.InlineIncrement;
import bsx.compiler.ast.name.Property;
import bsx.compiler.ast.name.VariableName;
import bsx.compiler.ast.operator.BinaryOperator;
import bsx.compiler.ast.operator.UnaryOperator;
import bsx.compiler.ast.types.TypeCast;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Types;
import bsx.util.Bytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

public class ExpressionCompiler {
    
    // Put expression result on the stack
    public static InsnList compile(CompilerContext ctx, BlockScope scope, Expression expression) {
        if (expression instanceof Literal literal) {
            return LiteralCompiler.compile(ctx, scope, literal);
        } else if (expression instanceof TypeCast cast) {
            return compileTypeCast(ctx, scope, cast);
        } else if (expression instanceof UnaryOperator uop) {
            return OperatorCompiler.compileUnary(ctx, scope, uop);
        } else if (expression instanceof BinaryOperator bop) {
            return OperatorCompiler.compileBinary(ctx, scope, bop);
        } else if (expression instanceof VariableName var) {
            return InvokeExpressionCompiler.compileVariable(ctx, scope, var);
        } else if (expression instanceof InlineIncrement inc) {
            return InvokeExpressionCompiler.compileInlineIncrement(ctx, scope, inc);
        } else if (expression instanceof ObjectCreation oc) {
            return InvokeExpressionCompiler.compileObjectCreation(ctx, scope, oc);
        } else if (expression instanceof Property property) {
            return InvokeExpressionCompiler.compileProperty(ctx, scope, property);
        } else if (expression instanceof ApplyCall call) {
            return InvokeExpressionCompiler.compileCall(ctx, scope, call);
        } else {
            throw new IllegalArgumentException("Can't compile expression of type " + expression.getClass());
        }
    }
    
    private static InsnList compileTypeCast(CompilerContext ctx, BlockScope scope, TypeCast expression) {
        InsnList instructions = new InsnList();
        instructions.add(compile(ctx, scope, expression.expr()));
        instructions.add(new LdcInsnNode(CompilerConstants.typeConstant(ctx, expression.type())));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Types.class.getMethod("castType", BsValue.class, BsType.class)));
        return instructions;
    }
}
