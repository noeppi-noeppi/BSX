package bsx.compiler.jvm.util;

import org.objectweb.asm.tree.InsnList;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public record CompilerContext(boolean hasThisLiteral, @Nullable Supplier<InsnList> returnCode, ClassData data) {
    
    public CompilerContext withThis(boolean hasThisLiteral) {
        return new CompilerContext(hasThisLiteral, this.returnCode(), this.data());
    }
    
    public CompilerContext withReturn(@Nullable Supplier<InsnList> returnCode) {
        return new CompilerContext(this.hasThisLiteral(), returnCode, this.data());
    }
    
    public CompilerContext withClassData(ClassData data) {
        return new CompilerContext(this.hasThisLiteral(), this.returnCode(), data);
    }
}
