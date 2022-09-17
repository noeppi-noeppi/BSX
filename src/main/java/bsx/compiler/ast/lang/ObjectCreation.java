package bsx.compiler.ast.lang;

import bsx.BsType;
import bsx.compiler.ast.Expression;

import java.util.List;

public record ObjectCreation(BsType type, List<Expression> args) implements Expression {
    
}
