package bsx.compiler.ast.lang;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;

import java.util.List;

public record BranchCondition(Expression condition, List<Statement> ifTrue, List<Statement> ifFalse) implements Statement {
    
}
