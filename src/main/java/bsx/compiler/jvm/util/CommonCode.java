package bsx.compiler.jvm.util;

import bsx.BsType;
import bsx.BsValue;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.Expression;
import bsx.compiler.ast.types.TypeHint;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.lvt.BlockScope;
import bsx.invoke.Calls;
import bsx.invoke.Language;
import bsx.invoke.Types;
import bsx.util.Bytecode;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.BiFunction;

public class CommonCode {
    
    public static InsnList lineNumber(int line) {
        InsnList instructions = new InsnList();
        LabelNode lineLabel = new LabelNode();
        lineLabel.getLabel(); // Resolve label
        instructions.add(lineLabel);
        instructions.add(new LineNumberNode(line, lineLabel));
        return instructions;
    }
    
    // Expects BsValue on stack, stack is left unchanged
    public static InsnList typeCheck(CompilerContext ctx, @Nullable TypeHint hint) {
        return typeCheck(ctx.data(), hint);
    }
    
    public static InsnList typeCheck(ClassData data, @Nullable TypeHint hint) {
        InsnList instructions = new InsnList();
        if (hint != null) {
            instructions.add(new LdcInsnNode(CompilerConstants.typeConstant(data, hint.type())));
            instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Types.class.getMethod("checkType", BsValue.class, BsType.class)));
        }
        return instructions;
    }
    
    // Put a new BsValue array with all expression results on the stack
    public static InsnList makeExpressionArray(CompilerContext ctx, BlockScope scope, List<Expression> expressions) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(expressions.size()));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getType(Object.class).getInternalName()));
        for (int i = 0; i < expressions.size(); i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new LdcInsnNode(i));
            instructions.add(ExpressionCompiler.compile(ctx, scope, expressions.get(i)));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        return instructions;
    }
    
    // name, args -> result
    public static InsnList makePredefCall(boolean special) {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(special ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("callPredef", String.class, Object[].class, boolean.class)));
        return instructions;
    }
    
    // localClass, self, name, args -> result
    public static InsnList makeLocalCall(boolean special) {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(special ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("callLocal", Class.class, Object.class, String.class, Object[].class, boolean.class)));
        return instructions;
    }
    
    // type, name, args -> result
    public static InsnList makeStaticCall(boolean special) {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(special ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("callType", BsType.class, String.class, Object[].class, boolean.class)));
        return instructions;
    }
    
    // value, name, args -> result
    public static InsnList makeInstanceCall(boolean special) {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(special ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Calls.class.getMethod("callValue", BsValue.class, String.class, Object[].class, boolean.class)));
        return instructions;
    }
    
    public static InsnList compileIntoBlock(CompilerContext ctx, BlockScope scope, BiFunction<CompilerContext, BlockScope, MethodNode> code) {
        return compileIntoBlock(ctx, scope, ctx.hasThisLiteral(), code);
    }
    
    // code may not set method access or desc
    public static InsnList compileIntoBlock(CompilerContext ctx, BlockScope scope, boolean instance, BiFunction<CompilerContext, BlockScope, MethodNode> code) {
        if (instance && !ctx.hasThisLiteral()) {
            throw new IllegalArgumentException("Can't compile instance block from static context");
        }

        MethodType methodType = scope.innerBlockType();
        BlockScope childScope = new BlockScope(instance, 0, scope); // No offset as all args are Variables
        CompilerContext childCtx = ctx.withThis(instance).withReturn(null);
        
        MethodNode node = code.apply(childCtx, childScope);
        node.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_MANDATED;
        if (!instance) node.access |= Opcodes.ACC_STATIC;
        if (node.name == null) node.name = "block";
        node.desc = Bytecode.getType(methodType).getDescriptor();
        
        childScope.checkDeleted();
        
        Handle handle = ctx.data().addHelperMethod(node);
        return scope.makeBlock(handle);
    }
    
    public static InsnList createVariableAt(String name, int lvt) {
        InsnList instructions = new InsnList();
        instructions.add(new LdcInsnNode(name));
        instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Language.class.getMethod("makeVariable", String.class)));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, lvt));
        return instructions;
    }
}
