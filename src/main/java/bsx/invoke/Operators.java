package bsx.invoke;

import bsx.BSX;
import bsx.BsValue;
import bsx.type.BoolType;
import bsx.type.StringType;
import bsx.util.LooseEquality;
import bsx.value.*;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.stream.Stream;

public class Operators {
    
    public static BsValue concat(BsValue op1, BsValue op2) {
        if (op1 instanceof StringValue sv1 && op2 instanceof StringValue sv2) {
            if (sv1.getType() == StringType.ASCII) {
                return StringValue.concatWith(sv2.getType(), sv1, sv2);
            } else if (sv2.getType() == StringType.ASCII) {
                return StringValue.concatWith(sv1.getType(), sv1, sv2);
            } else if (sv1.getType() == sv2.getType()) {
                return StringValue.concatWith(sv1.getType(), sv1, sv2);
            } else {
                throw new IllegalArgumentException("Can't concat " + sv1.getType() + " and " + sv2.getType());
            }
        } else if (op1 instanceof StringValue sv1) {
            return StringValue.concatWith(sv1.getType(), sv1, new StringValue(StringType.UTF256, op2.toString()));
        } else if (op2 instanceof StringValue sv2) {
            return StringValue.concatWith(sv2.getType(), new StringValue(StringType.UTF256, op1.toString()), sv2);
        } else {
            throw new IllegalArgumentException("Can't concat " + op1.getType() + " and " + op2.getType());
        }
    }
    
    public static BsValue invert(BsValue op) {
        if (op instanceof BoolValue bv) {
            return BoolValue.of(!bv.value);
        } else {
            return invert(BoolType.INSTANCE.cast(op));
        }
    }
    
    public static BsValue negate(BsValue op) {
        if (op instanceof IntegerValue iv) {
            return new IntegerValue(-iv.value);
        } else if (op instanceof FloatingValue fv) {
            return new FloatingValue(-fv.value);
        } else {
            OptionalDouble looseNum = LooseEquality.getNum(op);
            if (looseNum.isPresent()) {
                double d = -looseNum.getAsDouble();
                if (d == (int) d && (int) d == new IntegerValue((int) d).value) {
                    return new IntegerValue((int) d);
                } else {
                    return new FloatingValue(d);
                }
            } else {
                return FloatingValue.NaN;
            }
        }
    }
    
    public static BsValue equals(BsValue op1, BsValue op2) {
        return BoolValue.of(LooseEquality.equals(op1, op2));
    }

    public static BsValue notEquals(BsValue op1, BsValue op2) {
        return BoolValue.of(!LooseEquality.equals(op1, op2));
    }
    
    public static BsValue strictEquals(BsValue op1, BsValue op2) {
        return BoolValue.of(Objects.equals(op1, op2));
    }
    
    public static BsValue strictNotEquals(BsValue op1, BsValue op2) {
        return BoolValue.of(!Objects.equals(op1, op2));
    }
    
    public static BsValue lower(BsValue op1, BsValue op2) {
        double num1 = Double.NaN;
        if (op1 instanceof IntegerValue iv) num1 = iv.value;
        else if (op1 instanceof FloatingValue fv) num1 = fv.value;
        
        double num2 = Double.NaN;
        if (op2 instanceof IntegerValue iv) num2 = iv.value;
        else if (op2 instanceof FloatingValue fv) num2 = fv.value;
        
        return BoolValue.of(num1 < num2);
    }
    
    public static BsValue lowerEqual(BsValue op1, BsValue op2) {
        double num1 = Double.NaN;
        if (op1 instanceof IntegerValue iv) num1 = iv.value;
        else if (op1 instanceof FloatingValue fv) num1 = fv.value;

        double num2 = Double.NaN;
        if (op2 instanceof IntegerValue iv) num2 = iv.value;
        else if (op2 instanceof FloatingValue fv) num2 = fv.value;

        return BoolValue.of(num1 <= num2);
    }
    
    public static BsValue greater(BsValue op1, BsValue op2) {
        return lower(op2, op1);
    }
    
    public static BsValue greaterEqual(BsValue op1, BsValue op2) {
        return lowerEqual(op2, op1);
    }
    
    public static BsValue plus(BsValue op1, BsValue op2) {
        if (op1 instanceof ArrayValue av1 && op2 instanceof ArrayValue av2) {
            return new ArrayValue(Stream.concat(av1.values().stream(), av2.values().stream()).toList());
        } else {
            return numberBasedOp(op1, op2, Integer::sum, Double::sum);
        }
    }
    
    public static BsValue minus(BsValue op1, BsValue op2) {
        return numberBasedOp(op1, op2, (a, b) -> a - b, (a, b) -> a - b);
    }
    
    public static BsValue multiply(BsValue op1, BsValue op2) {
        return numberBasedOp(op1, op2, (a, b) -> a * b, (a, b) -> a * b);
    }
    
    public static BsValue divide(BsValue op1, BsValue op2) {
        return numberBasedOp(op1, op2, (a, b) -> a / b, (a, b) -> a / b);
    }
    
    public static BsValue modulo(BsValue op1, BsValue op2) {
        return numberBasedOp(op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
    
    public static BsValue and(BsValue op1, MethodHandle blockOp2) {
        try {
            // MethodHandle to allow lazy eval
            if (BoolType.INSTANCE.isTrue(op1)) {
                BsValue op2 = (BsValue) blockOp2.invoke();
                return BoolValue.of(BoolType.INSTANCE.isTrue(op2));
            } else {
                return BoolValue.FALSE;
            }
        } catch (Throwable t) {
            BSX.UNSAFE.throwException(t);
            throw new Error();
        }
    }
    
    public static BsValue or(BsValue op1, MethodHandle blockOp2) {
        try {
            // MethodHandle to allow lazy eval
            if (BoolType.INSTANCE.isTrue(op1)) {
                return BoolValue.TRUE;
            } else {
                BsValue op2 = (BsValue) blockOp2.invoke();
                return BoolValue.of(BoolType.INSTANCE.isTrue(op2));
            }
        } catch (Throwable t) {
            BSX.UNSAFE.throwException(t);
            throw new Error();
        }
    }
    
    private static BsValue numberBasedOp(BsValue op1, BsValue op2, IntBinaryOperator ints, DoubleBinaryOperator floats) {
        if (op1 instanceof IntegerValue iv1 && op2 instanceof IntegerValue iv2) {
            return new IntegerValue(ints.applyAsInt(iv1.value, iv2.value));
        } else if (op1 instanceof IntegerValue iv1 && op2 instanceof FloatingValue fv2) {
            return new FloatingValue(floats.applyAsDouble(iv1.value, fv2.value));
        } else if (op1 instanceof FloatingValue fv1 && op2 instanceof IntegerValue iv2) {
            return new FloatingValue(floats.applyAsDouble(fv1.value, iv2.value));
        } else if (op1 instanceof FloatingValue fv1 && op2 instanceof FloatingValue fv2) {
            return new FloatingValue(floats.applyAsDouble(fv1.value, fv2.value));
        } else {
            return FloatingValue.NaN;
        }
    }
}
