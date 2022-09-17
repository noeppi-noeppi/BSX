package bsx.compiler.ast.lang;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.Statement;

import javax.annotation.Nullable;

public record Return(@Nullable Expression expression) implements Statement {
    
}
