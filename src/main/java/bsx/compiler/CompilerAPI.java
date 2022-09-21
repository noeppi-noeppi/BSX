package bsx.compiler;

import bsx.compiler.ast.Program;
import bsx.compiler.lvt.Scope;
import bsx.load.LoadingContext;

import javax.annotation.Nullable;

public interface CompilerAPI {
    
    String preprocess(String code);
    String tokenize(String preprocessedCode);
    Program parseAST(String preprocessedCode);
    CompiledProgram compile(@Nullable String sourceFileName, Program program, LoadingContext context, @Nullable Scope scope);
    
    default CompiledProgram compile(Program program, LoadingContext context, @Nullable Scope scope) {
        return this.compile(null, program, context, scope);
    }
    
    static CompilerAPI get() {
        return CompilerImpl.INSTANCE;
    }
}
