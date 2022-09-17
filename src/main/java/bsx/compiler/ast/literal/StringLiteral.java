package bsx.compiler.ast.literal;

import bsx.type.StringType;

public record StringLiteral(StringType type, String escapedString) implements Literal {
    
}
