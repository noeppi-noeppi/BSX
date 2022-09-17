package bsx.compiler.ast.literal;

import bsx.compiler.ast.Expression;

import java.util.List;

public record InterpolatedUtfLiteral(List<Entry> entries) implements Literal {

    public sealed interface Entry {}
    public record ConstantEntry(String escapedUtf256) implements Entry {}
    public record ExpressionEntry(Expression expression) implements Entry {}
}
