package bsx.compiler.ast.name;

import bsx.compiler.ast.Expression;

public record InlineIncrement(VariableName var, boolean increment, boolean incrementFirst) implements Expression {
    
}
