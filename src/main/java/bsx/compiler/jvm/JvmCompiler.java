package bsx.compiler.jvm;

import bsx.BsType;
import bsx.BsValue;
import bsx.compiler.CompiledProgram;
import bsx.compiler.CompilerConstants;
import bsx.compiler.ast.BsClass;
import bsx.compiler.ast.Member;
import bsx.compiler.ast.Program;
import bsx.compiler.ast.Statement;
import bsx.compiler.ast.member.Function;
import bsx.compiler.ast.member.MemberModifier;
import bsx.compiler.ast.member.Property;
import bsx.compiler.jvm.statement.StatementCompiler;
import bsx.compiler.jvm.util.ClassData;
import bsx.compiler.jvm.util.CompilerContext;
import bsx.compiler.lvt.BlockScope;
import bsx.compiler.lvt.Scope;
import bsx.type.ClassType;
import bsx.util.Bytecode;
import bsx.value.NoValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JvmCompiler {
    
    public static CompiledProgram compile(@Nullable String sourceFileName, Program program, @Nullable Scope scope) {
        Set<String> internalClassNames = program.contents().stream()
                .flatMap(Program.Entry::asClass)
                .map(JvmCompiler::getInternalClassName)
                .collect(Collectors.toUnmodifiableSet());
        Predicate<String> internalNameExists = internalClassNames::contains;
        
        List<ClassNode> classes = program.contents().stream()
                .flatMap(Program.Entry::asClass)
                .map(cls -> compileClass(sourceFileName, cls, internalNameExists))
                .toList();
        List<Statement> statements = program.contents().stream()
                .flatMap(Program.Entry::asStatement)
                .toList();
        List<Function> functions = program.contents().stream()
                .flatMap(Program.Entry::asFunction)
                .toList();
        if (statements.isEmpty() && functions.isEmpty() && scope == null) {
            return new CompiledProgram(null, classes);
        } else {
            ClassNode main = compileMainCode(sourceFileName, statements, functions, scope, internalNameExists);
            return new CompiledProgram(main, classes);
        }
    }

    private static ClassNode compileMainCode(@Nullable String sourceFileName, List<Statement> statements, List<Function> functions, @Nullable Scope parentScope, Predicate<String> internalNameExists) {
        BlockScope scope;
        MethodType methodType;
        if (parentScope != null) {
            BlockScope reconstructedParentScope = new BlockScope(parentScope);
            scope = new BlockScope(false, 0, reconstructedParentScope);
            methodType = reconstructedParentScope.innerBlockType();
        } else {
            scope = new BlockScope(false, 0);
            methodType = MethodType.methodType(BsValue.class);
        }
        
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "bs/lang/Main";
        node.superName = Type.getType(Object.class).getInternalName();
        if (sourceFileName != null) node.sourceFile = sourceFileName;
        
        ClassData data = new ClassData(node.name, false, internalNameExists);
        CompilerContext ctx = new CompilerContext(false, null, data);
        
        MethodNode method = new MethodNode();
        method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
        method.name = "main";
        method.desc = Bytecode.getType(methodType).getDescriptor();
        
        method.instructions.add(StatementCompiler.compile(ctx, scope, statements));
        
        method.instructions.add(new LdcInsnNode(CompilerConstants.valueConstant(NoValue.INSTANCE)));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        
        node.methods.add(method);

        scope.checkDeleted();
        
        for (Function function : functions) {
            if ("main".equals(function.name())) {
                throw new IllegalStateException("Can't declare global main function");
            }
            
            node.methods.add(ClassCompiler.compileFunction(function, data));
        }
        
        data.applyTo(node);
        
        return node;
    }
    
    private static ClassNode compileClass(@Nullable String sourceFileName, BsClass cls, Predicate<String> internalNameExists) {
        BsType type = ClassType.makeCompileStatic(cls.name());
        if (!(type instanceof ClassType)) throw new IllegalStateException("Not a class type: " + type);
        String className = ((ClassType) type).typeName().replace(".", "/");
        
        BsType superType = ClassType.makeCompileStatic(cls.superName() == null ? "java.lang.Object" : cls.superName());
        if (!(superType instanceof ClassType)) throw new IllegalStateException("Not a class type: " + superType);
        String superName = ((ClassType) superType).typeName().replace(".", "/");
        
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        if (cls.modifiers().contains(MemberModifier.FINAL)) node.access |= Opcodes.ACC_FINAL;
        node.name = className;
        node.superName = superName;
        if (sourceFileName != null) node.sourceFile = sourceFileName;
        
        ClassData data = new ClassData(className, true, internalNameExists);
        
        for (Member member : cls.members()) {
            if (member instanceof Property property) {
                node.fields.add(ClassCompiler.compileProperty(property, data));
            }
            if (member instanceof Function function) {
                node.methods.add(ClassCompiler.compileFunction(function, data));
            }
        }
        
        data.applyTo(node);
        
        return node;
    }
    
    private static String getInternalClassName(BsClass cls) {
        BsType type = ClassType.makeCompileStatic(cls.name());
        if (!(type instanceof ClassType)) throw new IllegalStateException("Not a class type: " + type);
        return ((ClassType) type).typeName().replace(".", "/");
    }
}