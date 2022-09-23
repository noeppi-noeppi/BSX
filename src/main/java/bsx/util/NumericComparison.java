package bsx.util;

import bsx.BsValue;
import bsx.value.*;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

public class NumericComparison {
    
    public static OptionalInt compare(BsValue op1, BsValue op2) {
        if (Objects.equals(op1, op2)) return OptionalInt.of(0);
        
        OptionalDouble num1 = getNumericValue(op1);
        OptionalDouble num2 = getNumericValue(op2);
        if (num1.isPresent() && num2.isPresent()) {
            return OptionalInt.of(Double.compare(num1.getAsDouble(), num2.getAsDouble()));
        }
        
        if (op1 instanceof StringValue sv1 && op2 instanceof StringValue sv2) {
            return OptionalInt.of(StreamHelper.compare(sv1.getRaw(), sv2.getRaw()));
        }
        
        if (op1 instanceof ArrayValue av1 && op2 instanceof ArrayValue av2) {
            AtomicBoolean canCompare = new AtomicBoolean(true);
            int result = StreamHelper.compare(
                    av1.values().stream(),
                    av2.values().stream(),
                    (a, b) -> {
                        OptionalInt cmp = compare(a, b);
                        if (cmp.isPresent()) return cmp.getAsInt();
                        canCompare.set(false);
                        return 0;
                    }
            );
            return canCompare.get() ? OptionalInt.of(result) : OptionalInt.empty();
        }
        
        if (op1 instanceof ObjectValue ov1 && op2 instanceof ObjectValue ov2) {
            OptionalInt result = compareObjects(ov1.value, ov2.value);
            if (result.isPresent()) return result;
        }
        
        return OptionalInt.empty();
    }
    
    private static OptionalDouble getNumericValue(BsValue value) {
        if (value instanceof IntegerValue iv) return OptionalDouble.of(iv.value);
        if (value instanceof FloatingValue fv) return OptionalDouble.of(fv.value);
        return OptionalDouble.empty();
    }
    
    private static OptionalInt compareObjects(Object obj1, Object obj2) {
        if (obj1.getClass() != obj2.getClass()) return OptionalInt.empty();
        if (Comparable.class.isAssignableFrom(obj1.getClass())) {
            try {
                //noinspection unchecked
                return OptionalInt.of(((Comparable<Object>) obj1).compareTo(obj2));
            } catch (ClassCastException e) {
                return OptionalInt.empty();
            }
        } else {
            return OptionalInt.empty();
        }
    }
}
