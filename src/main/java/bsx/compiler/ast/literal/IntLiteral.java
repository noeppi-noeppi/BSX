package bsx.compiler.ast.literal;

import bsx.value.IntegerValue;

public record IntLiteral(int value) implements Literal {
    
    public static IntLiteral create(String value) {
        try {
            int i = Integer.parseInt(value);
            if (i != new IntegerValue(i).value) {
                throw new IllegalStateException("Integer literal out of range: " + value);
            }
            return new IntLiteral(i);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Integer literal out of range: " + value);
        }
    }
}
