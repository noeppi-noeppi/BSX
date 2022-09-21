package bsx.compiler.ast.lang;

import bsx.BsType;
import bsx.compiler.ast.Expression;

public record InstanceCheck(Expression expr, BsType type) implements Expression {
    
}
