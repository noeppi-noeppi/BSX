package bsx.compiler.ast.literal;

public record FloatLiteral(double value) implements Literal {

    public static FloatLiteral create(String value) {
        try {
            double d = Double.parseDouble(value);
            return new FloatLiteral(d);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid float literal: " + value);
        }
    }
}
