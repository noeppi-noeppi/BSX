package bsx.compiler.jvm.expression;

import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.lang.ObjectCreation;
import bsx.compiler.ast.literal.ThisLiteral;
import bsx.compiler.ast.name.*;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Values;
import bsx.util.Bytecode;
import bsx.variable.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

public class InvokeExpressionCompiler {
    
    public static InsnList compileVariable(CompilerContext ctx, BlockScope scope, VariableName var) {
        InsnList instructions = new InsnList();
        int lvt = scope.getVariable(var);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, lvt));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKEVIRTUAL, () -> Variable.class.getMethod("get")));
        return instructions;
    }
    
    public static InsnList compileInlineIncrement(CompilerContext ctx, BlockScope scope, InlineIncrement inc) {
        InsnList instructions = new InsnList();
        int lvt = scope.getVariable(inc.var());
        instructions.add(new VarInsnNode(Opcodes.ALOAD, lvt));
        instructions.add(new InsnNode(inc.increment() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(new InsnNode(inc.incrementFirst() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Values.class.getMethod("inlineIncrement", Variable.class, boolean.class, boolean.class)));
        return instructions;
    }
    
    public static InsnList compileObjectCreation(CompilerContext ctx, BlockScope scope, ObjectCreation oc) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(CompilerConstants.typeConstant(ctx, oc.type())));
        instructions.add(new LdcInsnNode("__construct"));
        instructions.add(CommonCode.makeExpressionArray(ctx, scope, oc.args()));
        instructions.add(CommonCode.makeStaticCall(false));
        return instructions;
    }
    
    public static InsnList compileProperty(CompilerContext ctx, BlockScope scope, Property property) {
        return compilePropertyAccess(ctx, scope, property, true, List.of());
    }
    
    public static InsnList compileCall(CompilerContext ctx, BlockScope scope, ApplyCall call) {
        if (call.expr() instanceof Property property) {
            return compilePropertyAccess(ctx, scope, property, false, call.args());
        } else {
            InsnList instructions = new InsnList();
            instructions.add(ExpressionCompiler.compile(ctx, scope, call.expr()));
            instructions.add(new LdcInsnNode("__invoke"));
            instructions.add(CommonCode.makeExpressionArray(ctx, scope, call.args()));
            instructions.add(CommonCode.makeInstanceCall(false));
            return instructions;
        }
    }
    
    @SuppressWarnings("ConstantConditions")
    public static InsnList compilePropertyAccess(CompilerContext ctx, BlockScope scope, Property property, boolean special, List<Expression> args) {
        InsnList instructions = new InsnList();
        
        if ("EVALUATE".equals(property.name())) {
            // Possibly a string evaluation, take scope snapshot
            instructions.add(CompilerConstants.takeSnapshot(scope));
        }
        
        if (property instanceof Name) {
            instructions.add(new LdcInsnNode(Type.getObjectType(ctx.data().className)));
            if (ctx.hasThisLiteral()) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            } else {
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            }
        } else if (property instanceof InstanceProperty ip) {
            instructions.add(ExpressionCompiler.compile(ctx, scope, ip.expr()));
        } else if (property instanceof StaticProperty sp) {
            instructions.add(new LdcInsnNode(CompilerConstants.typeConstant(ctx, sp.type())));
        } else if (property instanceof ParentProperty) {
            instructions.add(ExpressionCompiler.compile(ctx, scope, ThisLiteral.INSTANCE));
        } else {
            throw new IllegalArgumentException("Invalid property");
        }
        
        instructions.add(new LdcInsnNode((property instanceof ParentProperty ? "super@" : "") + property.name()));
        instructions.add(CommonCode.makeExpressionArray(ctx, scope, args));
        
        if (property instanceof Name) {
            instructions.add(CommonCode.makeLocalCall(special));
        } else if (property instanceof InstanceProperty || property instanceof ParentProperty) {
            instructions.add(CommonCode.makeInstanceCall(special));
        } else if (property instanceof StaticProperty) {
            instructions.add(CommonCode.makeStaticCall(special));
        } else {
            throw new IllegalArgumentException("Invalid property");
        }
        
        return instructions;
    }
}
