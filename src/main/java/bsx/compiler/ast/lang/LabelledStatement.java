package bsx.compiler.ast.lang;

import bsx.compiler.ast.Statement;

public record LabelledStatement(long label, Statement stmt) implements Statement {
    
}
