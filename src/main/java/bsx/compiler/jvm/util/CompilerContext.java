package bsx.compiler.jvm.util;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record CompilerContext(boolean hasThisLiteral, @Nullable Supplier<InsnList> returnCode, Consumer<LocalVariableNode> locals, ClassData data) {
    
    public CompilerContext withThis(boolean hasThisLiteral) {
        return new CompilerContext(hasThisLiteral, this.returnCode(), this.locals(), this.data());
    }
    
    public CompilerContext withReturn(@Nullable Supplier<InsnList> returnCode) {
        return new CompilerContext(this.hasThisLiteral(), returnCode, this.locals(), this.data());
    }
    
    public CompilerContext withLocals(Consumer<LocalVariableNode> locals) {
        return new CompilerContext(this.hasThisLiteral(), this.returnCode(), locals, this.data());
    }
    
    public CompilerContext withClassData(ClassData data) {
        return new CompilerContext(this.hasThisLiteral(), this.returnCode(), this.locals(), data);
    }
}
