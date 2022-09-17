package bsx.compiler.ast.name;

import bsx.compiler.ast.Expression;

import java.util.List;

public record ApplyCall(Expression expr, List<Expression> args) implements Expression {
    
}
