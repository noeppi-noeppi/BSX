package bsx.compiler.ast.operator;

import bsx.compiler.ast.Expression;

public record BinaryOperator(Type type, Expression arg1, Expression arg2) implements Expression {
    
    // Order matters for the parser
    public enum Type {
        CONCAT(",", Priority.CONCAT),
        STRICT_EQUAL("!!=!", Priority.EQUALITY),
        STRICT_NOT_EQUAL("!=!", Priority.EQUALITY),
        NOT_EQUAL("!=", Priority.EQUALITY),
        EQUAL("==", Priority.EQUALITY),
        LOWER_EQUAL("<=", Priority.RELATIONAL),
        LOWER("<", Priority.RELATIONAL),
        GREATER_EQUAL(">=", Priority.RELATIONAL),
        GREATER(">", Priority.RELATIONAL),
        PLUS("+", Priority.ADDITIVE),
        MINUS("-", Priority.ADDITIVE),
        MULTIPLY("*", Priority.MULTIPLICATIVE),
        DIVIDE("/", Priority.MULTIPLICATIVE),
        MODULO("%", Priority.MULTIPLICATIVE),
        LOGICAL_AND("&&", Priority.LOGICAL_AND),
        LOGICAL_OR("||", Priority.LOGICAL_OR);
        
        public final String name;
        public final Priority priority;

        Type(String name, Priority priority) {
            this.name = name;
            this.priority = priority;
        }
    }
    
    public enum Priority {
        MULTIPLICATIVE,
        ADDITIVE,
        RELATIONAL,
        EQUALITY,
        LOGICAL_AND,
        LOGICAL_OR,
        CONCAT;
        
        public final boolean isRightAssociative = false;
        
        public int priority() {
            return values().length - this.ordinal() - 1;
        }
    }
}
