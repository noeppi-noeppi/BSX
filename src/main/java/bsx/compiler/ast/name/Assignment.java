package bsx.compiler.ast.name;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;

public record Assignment(Expression target, Expression newValue) implements Statement {
    
}
