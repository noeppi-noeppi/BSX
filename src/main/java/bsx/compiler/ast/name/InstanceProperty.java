package bsx.compiler.ast.name;

import bsx.compiler.ast.Expression;

public record InstanceProperty(Expression expr, String name) implements Property {
    
}
