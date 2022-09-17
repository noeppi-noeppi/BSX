package bsx.compiler.ast;

import bsx.compiler.ast.member.Function;

import java.util.List;
import java.util.stream.Stream;

public record Program(List<Entry> contents) {
    
    public sealed interface Entry {
        default Stream<Line> asLine() { return Stream.empty(); }
        default Stream<Function> asFunction() { return Stream.empty(); }
        default Stream<BsClass> asClass() { return Stream.empty(); }
    }
    
    public record LineEntry(Line line) implements Entry {

        @Override
        public Stream<Line> asLine() {
            return Stream.of(this.line());
        }
    }
    
    public record FunctionEntry(Function func) implements Entry {

        @Override
        public Stream<Function> asFunction() {
            return Stream.of(this.func());
        }
    }
    
    public record ClassEntry(BsClass cls) implements Entry {

        @Override
        public Stream<BsClass> asClass() {
            return Stream.of(this.cls());
        }
    }
}
