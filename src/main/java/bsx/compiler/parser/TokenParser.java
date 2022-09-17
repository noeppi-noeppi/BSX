package bsx.compiler.parser;

import bsx.compiler.parser.antlr.BsLexer;
import bsx.compiler.parser.antlr.BsParser;
import org.antlr.v4.runtime.*;

import java.nio.IntBuffer;

public class TokenParser {
    
    private static TokenStream tokens(String code) {
        IntBuffer codePoints = IntBuffer.wrap(code.codePoints().toArray());
        BsLexer lexer = new BsLexer(CodePointCharStream.fromBuffer(CodePointBuffer.withInts(codePoints)));
        BufferedTokenStream tokens = new BufferedTokenStream(lexer);
        tokens.fill();
        return tokens;
    }
    
    public static String tokenize(String code) {
        TokenStream tokens = tokens(code);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            sb.append(BsLexer.VOCABULARY.getSymbolicName(token.getType()));
            sb.append(": ");
            sb.append(tokens.get(i).getText());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public static BsParser.ProgramContext parse(String code) {
        BsParser parser = new BsParser(tokens(code));
        ErrorListener errors = new ErrorListener();
        parser.addErrorListener(errors);
        BsParser.ProgramContext ctx = parser.program();
        if (errors.hasError) {
            throw new IllegalArgumentException("There were errors parsing the code.");
        }
        return ctx;
    }
    
    private static class ErrorListener extends BaseErrorListener {
        
        boolean hasError = false;
        
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            this.hasError = true;
        }
    }
}
