package bsx.compiler.jvm;

import bsx.BsValue;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.member.Function;
import bsx.compiler.ast.member.MemberModifier;
import bsx.compiler.ast.member.Parameter;
import bsx.compiler.ast.member.Property;
import bsx.compiler.jvm.expression.ExpressionCompiler;
import bsx.compiler.jvm.statement.StatementCompiler;
import bsx.compiler.jvm.util.ClassData;
import bsx.compiler.jvm.util.CommonCode;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.compiler.lvt.EmptyScope;
import bsx.invoke.Language;
import bsx.util.Bytecode;
import bsx.value.NoValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.function.Supplier;

public class ClassCompiler {
    
    public static FieldNode compileProperty(Property property, ClassData data) {
        int access = Opcodes.ACC_PUBLIC; // Ignore source visibility modifiers
        if (property.modifiers().contains(MemberModifier.STATIC)) access |= Opcodes.ACC_STATIC;
        if (property.modifiers().contains(MemberModifier.READONLY)) access |= Opcodes.ACC_FINAL;
        
        if (property.modifiers().contains(MemberModifier.READONLY) && property.initialValue() == null) {
            throw new IllegalStateException("A readonly property must have an initial value");
        }
        
        FieldNode node = new FieldNode(
                access, property.name(), Type.getType(BsValue.class).getDescriptor(),
                null, null
        );
        
        if ((access & Opcodes.ACC_STATIC) != 0) {
            CompilerContext ctx = new CompilerContext(false, null, data);
            InsnList instructions = new InsnList();
            if (property.initialValue() != null) {
                instructions.add(ExpressionCompiler.compile(ctx, EmptyScope.INSTANCE, property.initialValue()));
            } else {
                instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
            }
            instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, data.className, node.name, node.desc));
            data.addStaticInit(instructions);
        } else {
            CompilerContext ctx = new CompilerContext(true, null, data);
            InsnList instructions = new InsnList();
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (property.initialValue() != null) {
                instructions.add(ExpressionCompiler.compile(ctx, EmptyScope.INSTANCE, property.initialValue()));
            } else {
                instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
            }
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, data.className, node.name, node.desc));
            data.addInstanceInit(instructions);
        }
        
        return node;
    }
    
    public static MethodNode compileFunction(Function function, ClassData data) {
        MethodNode node = new MethodNode();
        Type methodType = makeMethodType(function.args().size());
        
        node.access = Opcodes.ACC_PUBLIC; // Ignore source visibility modifiers
        if (function.modifiers().contains(MemberModifier.STATIC)) node.access |= Opcodes.ACC_STATIC;
        if (function.modifiers().contains(MemberModifier.FINAL)) node.access |= Opcodes.ACC_FINAL;
        node.name = function.name();
        node.desc = methodType.getDescriptor();
        
        if ("__construct".equals(node.name)) {
            data.setHasConstructMethod();
        }
        
        BlockScope scope = new BlockScope((node.access & Opcodes.ACC_STATIC) == 0, function.args().size());
        
        // Wrap args into variables
        int staticOffset = (node.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (int i = 0; i < function.args().size(); i++) {
            Parameter arg = function.args().get(i);
            int varIdx = scope.newVariable(arg.variable());
            node.instructions.add(new LdcInsnNode(arg.variable().name()));
            node.instructions.add(new VarInsnNode(Opcodes.ALOAD, staticOffset + i));
            node.instructions.add(CommonCode.typeCheck(data, arg.hint()));
            node.instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Language.class.getMethod("makeVariable", String.class, BsValue.class)));
            node.instructions.add(new VarInsnNode(Opcodes.ASTORE, varIdx));
        }

        Supplier<InsnList> returnCode = () -> {
            InsnList instructions = new InsnList();
            instructions.add(CommonCode.typeCheck(data, function.returnTypeHint()));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return instructions;
        };
        
        CompilerContext ctx = new CompilerContext((node.access & Opcodes.ACC_STATIC) == 0, returnCode, data);
        
        node.instructions.add(StatementCompiler.compile(ctx, scope, function.statements()));
        
        // If there is no explicit return statement, return no value
        node.instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
        node.instructions.add(returnCode.get());
        
        scope.checkDeleted();
        
        return node;
    }
    
    private static Type makeMethodType(int args) {
        Type valueType = Type.getType(BsValue.class);
        Type[] argTypes = new Type[args];
        Arrays.fill(argTypes, valueType);
        return Type.getMethodType(valueType, argTypes);
    }
}
