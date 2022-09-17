package bsx.compiler.parser;

import bsx.compiler.ast.Program;
import bsx.compiler.parser.antlr.BsParser;
import bsx.compiler.parser.ast.AstConverter;

public class AstParser {
    
    public static Program parseAST(String preprocessedCode) {
        return parseAST(TokenParser.parse(preprocessedCode));
    }
    
    public static Program parseAST(BsParser.ProgramContext ctx) {
        return new AstConverter().program(ctx);
    }
}
