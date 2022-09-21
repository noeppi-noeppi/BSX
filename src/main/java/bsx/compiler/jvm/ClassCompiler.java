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

import java.util.ArrayList;
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
            CompilerContext ctx = new CompilerContext(false, null, lvt -> {}, data);
            InsnList instructions = new InsnList();
            instructions.add(CommonCode.lineNumber(property.lineNumber()));
            if (property.initialValue() != null) {
                instructions.add(ExpressionCompiler.compile(ctx, EmptyScope.INSTANCE, property.initialValue()));
            } else {
                instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
            }
            instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, data.className, node.name, node.desc));
            data.addStaticInit(instructions);
        } else {
            CompilerContext ctx = new CompilerContext(true, null, lvt -> {}, data);
            InsnList instructions = new InsnList();
            instructions.add(CommonCode.lineNumber(property.lineNumber()));
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
    
    public static MethodNode compileFunction(Function function, ClassData data, boolean isInterface) {
        if (isInterface && !function.modifiers().contains(MemberModifier.STATIC) && !function.lines().isEmpty()) {
            throw new IllegalArgumentException("Interface function can't contain code");
        }
        
        MethodNode node = new MethodNode();
        Type methodType = makeMethodType(function.args().size());
        
        node.access = Opcodes.ACC_PUBLIC; // Ignore source visibility modifiers
        if (function.modifiers().contains(MemberModifier.STATIC)) node.access |= Opcodes.ACC_STATIC;
        if (isInterface && !function.modifiers().contains(MemberModifier.STATIC)) {
            if (function.modifiers().contains(MemberModifier.FINAL)) throw new IllegalStateException("Final function in interface");
            node.access |= Opcodes.ACC_ABSTRACT;
        } else {
            if (function.modifiers().contains(MemberModifier.FINAL)) node.access |= Opcodes.ACC_FINAL;
        }
        if ("__toString".equals(function.name()) && function.args().size() == 0 && !function.modifiers().contains(MemberModifier.STATIC)) {
            // Special case: Compile PHP toString method to use the java name.
            // resolution will take care that this method can be found with __toString as well.
            node.name = "toString";
        } else {
            node.name = function.name();
        }
        node.desc = methodType.getDescriptor();
        
        if ("__construct".equals(node.name)) {
            if (isInterface) {
                throw new IllegalStateException("Constructor declaration in interface");
            } else {
                data.setHasConstructMethod();
            }
        }
        
        if ((node.access & Opcodes.ACC_STATIC) != 0 || !isInterface) {
            BlockScope scope = new BlockScope((node.access & Opcodes.ACC_STATIC) == 0, function.args().size());

            node.instructions.add(CommonCode.lineNumber(function.lineNumber()));
            node.parameters = new ArrayList<>();
            node.localVariables = new ArrayList<>();

            LabelNode startLabel = new LabelNode();
            startLabel.getLabel();

            node.instructions.add(startLabel);

            // Wrap args into variables
            int staticOffset = (node.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            for (int i = 0; i < function.args().size(); i++) {
                Parameter arg = function.args().get(i);
                LocalVariableNode lvt = scope.newVariable(arg.variable(), startLabel);
                node.parameters.add(new ParameterNode(arg.variable().name(), 0));
                node.localVariables.add(lvt);
                node.instructions.add(new LdcInsnNode(arg.variable().name()));
                node.instructions.add(new VarInsnNode(Opcodes.ALOAD, staticOffset + i));
                node.instructions.add(CommonCode.typeCheck(data, arg.hint()));
                node.instructions.add(Bytecode.methodCall(Opcodes.INVOKESTATIC, () -> Language.class.getMethod("makeVariable", String.class, BsValue.class)));
                node.instructions.add(new VarInsnNode(Opcodes.ASTORE, lvt.index));
            }

            Supplier<InsnList> returnCode = () -> {
                InsnList instructions = new InsnList();
                instructions.add(CommonCode.typeCheck(data, function.returnTypeHint()));
                instructions.add(new InsnNode(Opcodes.ARETURN));
                return instructions;
            };

            CompilerContext ctx = new CompilerContext((node.access & Opcodes.ACC_STATIC) == 0, returnCode, node.localVariables::add, data);

            node.instructions.add(StatementCompiler.compile(ctx, scope, function.lines()));

            // If there is no explicit return statement, return no value
            node.instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
            node.instructions.add(returnCode.get());

            node.instructions.add(scope.end());
        } else {
            node.parameters = new ArrayList<>();
            for (int i = 0; i < function.args().size(); i++) {
                Parameter arg = function.args().get(i);
                node.parameters.add(new ParameterNode(arg.variable().name(), 0));
            }
        }
        
        return node;
    }
    
    private static Type makeMethodType(int args) {
        Type valueType = Type.getType(BsValue.class);
        Type[] argTypes = new Type[args];
        Arrays.fill(argTypes, valueType);
        return Type.getMethodType(valueType, argTypes);
    }
}
