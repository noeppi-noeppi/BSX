package bsx.compiler.jvm.util;

import bsx.resolution.NoLookup;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ClassData {
    
    public final String className;
    private final boolean hasInit;
    private final boolean isInterface;
    private final InsnList instanceInit;
    private final InsnList staticInit;
    private final List<MethodNode> blockMethods;
    private final Predicate<String> internalNameExists;
    
    private int nextId = 0;
    private boolean hasConstructMethod = false;

    public ClassData(String className, boolean hasInit, boolean isInterface, Predicate<String> internalNameExists) {
        this.className = className;
        this.hasInit = hasInit;
        this.isInterface = isInterface;
        this.instanceInit = new InsnList();
        this.staticInit = new InsnList();
        this.blockMethods = new ArrayList<>();
        this.internalNameExists = internalNameExists;
    }

    public void addInstanceInit(InsnList instructions) {
        if (!this.hasInit || this.isInterface) {
            throw new IllegalStateException("class data does not allow adding instance init code");
        }
        this.instanceInit.add(instructions);
    }

    public void addStaticInit(InsnList instructions) {
        if (!this.hasInit) {
            throw new IllegalStateException("class data does not allow adding static init code");
        }
        this.staticInit.add(instructions);
    }
    
    // Method access is set to private, name is decorated to be unique
    public Handle addHelperMethod(MethodNode method) {
        if (this.isInterface && (method.access & Opcodes.ACC_STATIC) == 0) {
            throw new IllegalStateException("Can't add non-static block method to interface");
        }
        method.access &= ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
        method.access |= Opcodes.ACC_PRIVATE;
        method.name = method.name + "$" + (this.nextId++);
        this.blockMethods.add(method);
        int handleTag = (method.access & Opcodes.ACC_STATIC) != 0 ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL;
        return new Handle(handleTag, this.className, method.name, method.desc, false);
    }
    
    public void setHasConstructMethod() {
        this.hasConstructMethod = true;
    }
    
    public boolean exists(String internalName) {
        return this.internalNameExists.test(internalName);
    }
    
    public void applyTo(ClassNode node) {
        if (!this.className.equals(node.name)) {
            throw new IllegalArgumentException("Mismatching class node");
        }
        
        node.methods.addAll(this.blockMethods);
        
        if (this.hasInit) {
            if (!this.isInterface) {
                // Generate default constructor and clinit
                MethodNode init = new MethodNode();
                init.access = Opcodes.ACC_PUBLIC;
                init.name = "<init>";
                init.desc = "()V";
                init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V"));
                init.instructions.add(this.instanceInit);
                init.instructions.add(new InsnNode(Opcodes.RETURN));
                if (this.hasConstructMethod) {
                    // Constructor should not be looked up directly, must use __construct
                    init.visibleAnnotations = new ArrayList<>();
                    init.visibleAnnotations.add(new AnnotationNode(Type.getType(NoLookup.class).getDescriptor()));
                }
                node.methods.add(0, init);
            }

            MethodNode clinit = new MethodNode();
            clinit.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            clinit.name = "<clinit>";
            clinit.desc = "()V";
            clinit.instructions.add(this.staticInit);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(0, clinit);
        } else if (!this.isInterface) {
            MethodNode init = new MethodNode();
            init.access = Opcodes.ACC_PRIVATE;
            init.name = "<init>";
            init.desc = "()V";
            init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V"));
            init.instructions.add(new InsnNode(Opcodes.RETURN));
            if (this.hasConstructMethod) {
                init.visibleAnnotations = new ArrayList<>();
                init.visibleAnnotations.add(new AnnotationNode(Type.getType(NoLookup.class).getDescriptor()));
            }
            node.methods.add(0, init);
        }
    }
}
