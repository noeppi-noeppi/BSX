package bsx.compiler.jvm.expression;

import bsx.BsValue;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.operator.BinaryOperator;
import bsx.compiler.ast.operator.UnaryOperator;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Operators;
import bsx.util.Bytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class OperatorCompiler {
    
    public static InsnList compileUnary(CompilerContext ctx, BlockScope scope, UnaryOperator expression) {
        InsnList instructions = new InsnList();
        instructions.add(ExpressionCompiler.compile(ctx, scope, expression.arg()));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> impl(expression.type())));
        return instructions;
    }
    
    public static InsnList compileBinary(CompilerContext ctx, BlockScope scope, BinaryOperator expression) {
        InsnList instructions = new InsnList();
        if (expression.type() == BinaryOperator.Type.LOGICAL_AND) {
            instructions.add(loadLazy(ctx, scope, "and", expression.arg1(), expression.arg2()));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Operators.class.getMethod("and", BsValue.class, MethodHandle.class)));
        } else if (expression.type() == BinaryOperator.Type.LOGICAL_OR) {
            instructions.add(loadLazy(ctx, scope, "or", expression.arg1(), expression.arg2()));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Operators.class.getMethod("or", BsValue.class, MethodHandle.class)));
        } else {
            instructions.add(ExpressionCompiler.compile(ctx, scope, expression.arg1()));
            instructions.add(ExpressionCompiler.compile(ctx, scope, expression.arg2()));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> impl(expression.type())));
        }
        return instructions;
    }
    
    private static InsnList loadLazy(CompilerContext ctx, BlockScope scope, String name, Expression arg1, Expression arg2) {
        InsnList instructions = new InsnList();
        instructions.add(ExpressionCompiler.compile(ctx, scope, arg1));
        instructions.add(CommonCode.compileIntoBlock(ctx, scope, (c, s) -> {
            MethodNode node = new MethodNode();
            node.name = name;
            node.instructions.add(ExpressionCompiler.compile(c, s, arg2));
            node.instructions.add(new InsnNode(Opcodes.ARETURN));
            return node;
        }));
        return instructions;
    }
    
    private static Method impl(UnaryOperator.Type type) throws ReflectiveOperationException {
        return switch (type) {
            case INVERT -> Operators.class.getMethod("invert", BsValue.class);
            case NEGATE -> Operators.class.getMethod("negate", BsValue.class);
        };
    }
    
    private static Method impl(BinaryOperator.Type type) throws ReflectiveOperationException {
        return switch (type) {
            case CONCAT -> Operators.class.getMethod("concat", BsValue.class, BsValue.class);
            case STRICT_EQUAL -> Operators.class.getMethod("strictEquals", BsValue.class, BsValue.class);
            case STRICT_NOT_EQUAL -> Operators.class.getMethod("strictNotEquals", BsValue.class, BsValue.class);
            case NOT_EQUAL -> Operators.class.getMethod("notEquals", BsValue.class, BsValue.class);
            case EQUAL -> Operators.class.getMethod("equals", BsValue.class, BsValue.class);
            case LOWER_EQUAL -> Operators.class.getMethod("lowerEqual", BsValue.class, BsValue.class);
            case LOWER -> Operators.class.getMethod("lower", BsValue.class, BsValue.class);
            case GREATER_EQUAL -> Operators.class.getMethod("greaterEqual", BsValue.class, BsValue.class);
            case GREATER -> Operators.class.getMethod("greater", BsValue.class, BsValue.class);
            case PLUS -> Operators.class.getMethod("plus", BsValue.class, BsValue.class);
            case MINUS -> Operators.class.getMethod("minus", BsValue.class, BsValue.class);
            case MULTIPLY -> Operators.class.getMethod("multiply", BsValue.class, BsValue.class);
            case DIVIDE -> Operators.class.getMethod("divide", BsValue.class, BsValue.class);
            case MODULO -> Operators.class.getMethod("modulo", BsValue.class, BsValue.class);
            default -> throw new IllegalArgumentException("Special operator has no impl method: " + type);
        };
    }
}
