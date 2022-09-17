package bsx.compiler.ast.name;

import bsx.BsType;

public record StaticProperty(BsType type, String name) implements Property {
    
}
