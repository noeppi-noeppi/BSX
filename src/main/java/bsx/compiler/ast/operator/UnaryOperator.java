package bsx.compiler.ast.operator;

import bsx.compiler.ast.Expression;

public record UnaryOperator(Type type, Expression arg) implements Expression {
    
    public enum Type {
        INVERT("-"),
        NEGATE("!");
        
        public final String name;

        Type(String name) {
            this.name = name;
        }
    }
}
