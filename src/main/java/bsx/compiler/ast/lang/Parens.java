package bsx.compiler.ast.lang;

import bsx.compiler.ast.Expression;

public record Parens(Expression expression) implements Expression {
    
}
