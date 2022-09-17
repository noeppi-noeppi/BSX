package bsx.util;

import bsx.BsValue;
import bsx.value.*;

import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

public class LooseEquality {
    
    public static boolean equals(BsValue op1, BsValue op2) {
        // If things are strictly equal, they are also loosely equal
        if (Objects.equals(op1, op2)) {
            return true;
        }
        
        // Special check for pie emoji
        if (op1 instanceof FloatingValue f1 && op2 instanceof FloatingValue f2) {
            boolean f1pi = f1.value == 22/7d || f1.isPieEmoji;
            boolean f2pi = f2.value == 22/7d || f2.isPieEmoji;
            return (f1pi && f2pi) || f1.value == f2.value;
        }
        
        // Two nullish values are equal
        if (op1 instanceof NullValue && op2 instanceof NullValue) {
            return true;
        }
        
        // First check two strings. Compared regardless of type
        // needs to be before number based equality, so '' != '0'
        if (op1 instanceof StringValue sv1 && op2 instanceof StringValue sv2) {
            return StringValue.contentEquals(sv1, sv2);
        }
        
        // Try numeric comparison
        OptionalDouble num1 = getNum(op1);
        OptionalDouble num2 = getNum(op2);
        if (num1.isPresent() && num2.isPresent()) {
            return num1.getAsDouble() == num2.getAsDouble();
        }
        
        return false;
    }
    
    public static OptionalDouble getNum(BsValue value) {
        if (value instanceof IntegerValue iv) {
            return OptionalDouble.of(iv.value);
        } else if (value instanceof FloatingValue fv) {
            return OptionalDouble.of(fv.value);
        } else if (value instanceof StringValue sv && sv.getPrintableString().isPresent()) {
            String str = sv.getPrintableString().get().toLowerCase(Locale.ROOT);
            if (str.isEmpty()) {
                return OptionalDouble.of(0);
            }
            try {
                return OptionalDouble.of(Long.parseLong(str));
            } catch (NumberFormatException e) {
                //
            }
            if (!"nan".equals(str) && str.contains("infinity")) {
                try {
                    return OptionalDouble.of(Double.parseDouble(str));
                } catch (NumberFormatException e) {
                    //
                }
            }
            return switch (str) {
                case "zero" -> OptionalDouble.of(0);
                case "one" -> OptionalDouble.of(1);
                case "two" -> OptionalDouble.of(2);
                case "three" -> OptionalDouble.of(3);
                case "four" -> OptionalDouble.of(4);
                case "five" -> OptionalDouble.of(5);
                case "six" -> OptionalDouble.of(6);
                case "seven" -> OptionalDouble.of(7);
                case "eight" -> OptionalDouble.of(8);
                case "nine" -> OptionalDouble.of(9);
                case "ten" -> OptionalDouble.of(10);
                case "eleven" -> OptionalDouble.of(11);
                case "twelve" -> OptionalDouble.of(12);
                default -> OptionalDouble.empty();
            };
        } else if (value instanceof BoolValue bv) {
            return OptionalDouble.of(bv.value ? 1 : 0);
        } else if (value instanceof ArrayValue av) {
            return av.length() == 0 ? OptionalDouble.of(0) : OptionalDouble.empty();
        } else {
            return OptionalDouble.empty();
        }
    }
}
