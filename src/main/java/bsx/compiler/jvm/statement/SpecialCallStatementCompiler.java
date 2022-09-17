package bsx.compiler.jvm.statement;

import bsx.BsValue;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.lang.*;
import bsx.compiler.ast.name.VariableName;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Calls;
import bsx.util.Bytecode;
import bsx.value.NoValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandle;

public class SpecialCallStatementCompiler {

    public static InsnList compileEcho(CompilerContext ctx, BlockScope scope, Labels labels, Echo statement) {
        InsnList instructions = new InsnList();
        instructions.add(ExpressionCompiler.compile(ctx, scope, statement.expression()));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("echo", BsValue.class)));
        return instructions;
    }
    
    public static InsnList compileGoto(CompilerContext ctx, BlockScope scope, Labels labels, Goto statement) {
        InsnList instructions = new InsnList();
        instructions.add(new JumpInsnNode(Opcodes.GOTO, labels.getLabel(statement.label())));
        return instructions;
    }

    public static InsnList compileReturn(CompilerContext ctx, BlockScope scope, Labels labels, Return statement) {
        InsnList instructions = new InsnList();
        if (ctx.returnCode() == null) throw new IllegalStateException("Can't return from here");
        if (statement.expression() == null) {
            instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
        } else {
            instructions.add(ExpressionCompiler.compile(ctx, scope, statement.expression()));
        }
        instructions.add(ctx.returnCode().get());
        return instructions;
    }

    public static InsnList compileDoAnd(CompilerContext ctx, BlockScope scope, Labels labels, DoAnd statement) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(statement.blocks().size()));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(MethodHandle.class).getInternalName()));
        for (int i = 0; i < statement.blocks().size(); i++) {
            DoAnd.Block block = statement.blocks().get(i);
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new LdcInsnNode(i));
            instructions.add(CommonCode.compileIntoBlock(ctx, scope, (c, s) -> {
                MethodNode node = new MethodNode();
                node.instructions.add(StatementCompiler.compile(c, s, block.lines()));
                node.instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
                node.instructions.add(new InsnNode(Opcodes.ARETURN));
                return node;
            }));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("doAnd", MethodHandle[].class)));
        return instructions;
    }

    public static InsnList compileDelete(CompilerContext ctx, BlockScope scope, Labels labels, Deletion statement) {
        for (VariableName var : statement.variables()) {
            scope.deleteVariable(var);
        }
        return new InsnList();
    }
}
