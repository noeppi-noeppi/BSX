package bsx.compiler.ast.literal;

import bsx.value.NullValue;

public record NullLiteral(NullValue type) implements Literal {
    
}
