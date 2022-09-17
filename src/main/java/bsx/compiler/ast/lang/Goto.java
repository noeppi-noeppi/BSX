package bsx.compiler.ast.lang;

import bsx.compiler.ast.Statement;

public record Goto(long label) implements Statement {
    
}
