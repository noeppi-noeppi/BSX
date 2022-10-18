package bsx.compiler.jvm.expression;

import bsx.BSX;
import bsx.BsValue;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.literal.*;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Values;
import bsx.type.StringType;
import bsx.util.Bytecode;
import bsx.util.string.StringEscapeHelper;
import bsx.value.BoolValue;
import bsx.value.FloatingValue;
import bsx.value.IntegerValue;
import bsx.value.StringValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LiteralCompiler {

    public static InsnList compile(CompilerContext ctx, BlockScope scope, Literal literal) {
        InsnList instructions = new InsnList();
        if (literal instanceof ThisLiteral) {
            if (!ctx.hasThisLiteral()) {
                throw new IllegalStateException("€this reference is unbound here.");
            }
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("wrapValue", Object.class)));
        } else if (literal instanceof PieLiteral) {
            instructions.add(new LdcInsnNode(BSX.PIE));
            instructions.add(CommonCode.makeExpressionArray(ctx, scope, List.of()));
            instructions.add(CommonCode.makePredefCall(true));
        } else if (literal instanceof InterpolatedUtfLiteral iul) {
            instructions.add(compileInterpolatedUtf256(ctx, scope, iul));
        } else {
            instructions.add(compileSimple(literal));
        }
        return instructions;
    }

    public static AbstractInsnNode compileSimple(Literal literal) {
        if (literal instanceof NullLiteral nl) {
            return new LdcInsnNode(CompilerConstants.valueConstant(nl.type()));
        } else if (literal instanceof BoolLiteral bl) {
            return new LdcInsnNode(CompilerConstants.valueConstant(BoolValue.of(bl.value())));
        } else if (literal instanceof IntLiteral il) {
            return new LdcInsnNode(CompilerConstants.valueConstant(new IntegerValue(il.value())));
        } else if (literal instanceof FloatLiteral fl) {
            return new LdcInsnNode(CompilerConstants.valueConstant(new FloatingValue(fl.value())));
        } else if (literal instanceof StringLiteral sl) {
            return new LdcInsnNode(CompilerConstants.valueConstant(new StringValue(sl.type(), StringEscapeHelper.unescape(sl.escapedString()))));
        } else if (literal instanceof UtfLiteral ul) {
            return compileUtf256(ul);
        } else {
            throw new IllegalArgumentException("Can't compile literal of type " + literal.getClass());
        }
    }
    
    public static AbstractInsnNode compileUtf256(UtfLiteral literal) {
        // Throw exception if there are invalid escapes
        int[] utf256 = StringEscapeHelper.unescapeUtf256(literal.escapedUtf256());
        StringValue value = new StringValue(StringType.UTF256, utf256);
        if (value.getPrintableString().isPresent()) {
            return new LdcInsnNode(CompilerConstants.valueConstant(value));
        } else {
            return new LdcInsnNode(CompilerConstants.utfConstant(literal.escapedUtf256()));
        }
    }
    
    public static InsnList compileInterpolatedUtf256(CompilerContext ctx, BlockScope scope, InterpolatedUtfLiteral literal) {
        StringBuilder sb = new StringBuilder();
        List<Expression> expressions = new ArrayList<>();
        for (InterpolatedUtfLiteral.Entry entry : literal.entries()) {
            if (entry instanceof InterpolatedUtfLiteral.ConstantEntry constant) {
                // Replace literal dollars with an escape as a dollar in the template string means insert a replacement
                sb.append(constant.escapedUtf256()
                        .replace("\\$", "\\u0024") // Dollars are escaped as a dollar inserts an expr if not escaped
                        .replace("$", "\\u0024")
                );
            } else if (entry instanceof InterpolatedUtfLiteral.ExpressionEntry expr) {
                sb.append("$");
                expressions.add(expr.expression());
            }
        }
        String template = sb.toString();
        
        InsnList instructions = new InsnList();
        
        if (expressions.isEmpty()) {
            instructions.add(compileUtf256(new UtfLiteral(template)));
        } else {
            Type[] argTypes = new Type[expressions.size()];
            Arrays.fill(argTypes, Type.getType(Object.class));
            Type methodType = Type.getMethodType(Type.getType(BsValue.class), argTypes);

            for (Expression expr : expressions) {
                instructions.add(ExpressionCompiler.compile(ctx, scope, expr));
            }

            instructions.add(new InvokeDynamicInsnNode(
                    "interpolate", methodType.getDescriptor(),
                    Bytecode.methodHandle(Opcodes.H_INVOKESTATIC, () -> Values.class.getMethod("makeUtf256ValueInterpolation", MethodHandles.Lookup.class, String.class, MethodType.class, String.class)),
                    template
            ));
        }
        
        return instructions;
    }
}
