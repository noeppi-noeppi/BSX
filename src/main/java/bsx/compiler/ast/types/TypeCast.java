package bsx.compiler.ast.types;

import bsx.BsType;
import bsx.compiler.ast.Expression;

public record TypeCast(BsType type, Expression expr) implements Expression {
    
}
