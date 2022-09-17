package bsx.compiler.ast.lang;

import bsx.compiler.ast.Statement;
import bsx.compiler.ast.name.VariableName;

import java.util.List;

public record Deletion(List<VariableName> variables) implements Statement {
    
}
