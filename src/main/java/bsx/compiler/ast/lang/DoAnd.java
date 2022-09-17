package bsx.compiler.ast.lang;

import bsx.compiler.ast.Line;
import bsx.compiler.ast.Statement;

import java.util.List;

public record DoAnd(List<Block> blocks) implements Statement {
    
    public record Block(List<Line> lines) {}
}
