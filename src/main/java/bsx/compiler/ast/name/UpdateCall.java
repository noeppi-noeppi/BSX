package bsx.compiler.ast.name;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;

import java.util.List;

public record UpdateCall(Expression expr, List<Expression> args, Expression newValue) implements Statement {
    
}
