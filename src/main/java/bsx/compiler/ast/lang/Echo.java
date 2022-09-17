package bsx.compiler.ast.lang;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;

public record Echo(Expression expression) implements Statement {
    
}
