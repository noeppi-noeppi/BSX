package bsx.compiler.ast.lang;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Line;
import bsx.compiler.ast.Statement;

import java.util.List;

public record BranchCondition(int conditionLineNumber, Expression condition, List<Line> ifTrue, List<Line> ifFalse) implements Statement {
    
}
