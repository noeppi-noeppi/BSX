package bsx.compiler;

import bsx.compiler.ast.Program;
import bsx.compiler.jvm.JvmCompiler;
import bsx.compiler.lvt.Scope;
import bsx.compiler.parser.AstParser;
import bsx.compiler.parser.TokenParser;
import bsx.compiler.preprocessor.PreProcessor;
import bsx.load.LoadingContext;

import javax.annotation.Nullable;

public class CompilerImpl implements CompilerAPI {

    public static final CompilerImpl INSTANCE = new CompilerImpl();

    private CompilerImpl() {

    }

    @Override
    public String preprocess(String code) {
        return PreProcessor.preprocess(code);
    }

    @Override
    public String tokenize(String preprocessedCode) {
        return TokenParser.tokenize(preprocessedCode);
    }

    @Override
    public Program parseAST(String preprocessedCode) {
        return AstParser.parseAST(preprocessedCode);
    }
    
    public CompiledProgram compile(@Nullable String sourceFileName, Program program, LoadingContext context, @Nullable Scope scope) {
        return JvmCompiler.compile(sourceFileName, program, context, scope);
    }
}
