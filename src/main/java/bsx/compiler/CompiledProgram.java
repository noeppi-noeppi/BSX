package bsx.compiler;

import bsx.Bootstrap;
import bsx.load.LoadingContext;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class CompiledProgram {
    
    @Nullable
    private final ClassNode mainCode;
    private final List<ClassNode> classes;

    public CompiledProgram(@Nullable ClassNode mainCode, List<ClassNode> classes) {
        this.mainCode = mainCode;
        this.classes = List.copyOf(classes);
    }
    
    public MethodHandle loadIntoCurrentEnvironment() {
        LoadingContext ctx = Bootstrap.context();
        for (ClassNode cls : this.classes) {
            ctx.registerClass(cls);
        }
        for (ClassNode cls : this.classes) {
            ctx.loadClass(cls);
        }
        if (this.mainCode == null) {
            return MethodHandles.empty(MethodType.methodType(void.class));
        } else {
            return ctx.registerAnLoadMain(this.mainCode);
        }
    }
}
