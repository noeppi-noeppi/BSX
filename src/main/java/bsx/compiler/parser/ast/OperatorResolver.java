package bsx.compiler.parser.ast;

import bsx.compiler.ast.Expression;
import bsx.compiler.ast.operator.BinaryOperator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OperatorResolver {
    
    public static Expression applyOperators(List<Expression> expressions, List<String> operators) {
        if (expressions.isEmpty()) throw new IllegalArgumentException("No expressions");
        if (expressions.size() != operators.size() + 1) throw new IllegalArgumentException("Invalid operator size, expected " + (expressions.size() - 1) + ", got " + operators.size());

        ArrayList<Expression> expr = new ArrayList<>(expressions);
        ArrayList<BinaryOperator.Type> op = new ArrayList<>(operators.stream().map(OperatorResolver::getTypeOrThrow).toList());
        
        for (BinaryOperator.Priority priority : BinaryOperator.Priority.values()) {
            applyPriority(priority, expr, op);
        }
        
        if (expr.size() != 1) throw new IllegalStateException("Failed to resolve all operators, left: " + expr + " (op: " + op + ")");
        
        return expr.get(0);
    }
    
    private static void applyPriority(BinaryOperator.Priority priority, ArrayList<Expression> expr, ArrayList<BinaryOperator.Type> op) {
        if (priority.isRightAssociative) {
            for (int i = op.size() - 1; i >= 0; i--) {
                BinaryOperator.Type type = op.get(i);
                if (type.priority == priority) {
                    op.remove(i);
                    BinaryOperator newExpr = new BinaryOperator(type, expr.get(i), expr.get(i + 1));
                    expr.remove(i + 1);
                    expr.set(i, newExpr);
                }
            }
        } else {
            for (int i = 0; i < op.size(); i++) {
                BinaryOperator.Type type = op.get(i);
                if (type.priority == priority) {
                    op.remove(i);
                    BinaryOperator newExpr = new BinaryOperator(type, expr.get(i), expr.get(i + 1));
                    expr.remove(i + 1);
                    expr.set(i, newExpr);
                    i -= 1;
                }
            }
        }
    }
    
    private static BinaryOperator.Type getTypeOrThrow(String name) {
        BinaryOperator.Type type = getType(name);
        if (type == null) throw new IllegalArgumentException("Invalid operator: " + name);
        return type;
    }
    
    @Nullable
    public static BinaryOperator.Type getType(String name) {
        for (BinaryOperator.Type type : BinaryOperator.Type.values()) {
            if (type.name.equals(name)) return type;
        }
        return null;
    }
}
