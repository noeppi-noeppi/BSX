package bsx.invoke;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.BoolType;
import bsx.type.StringType;
import bsx.util.MethodUtil;
import bsx.util.string.StringEscapeHelper;
import bsx.util.string.Utf256Joiner;
import bsx.value.*;
import bsx.variable.Variable;

import java.lang.invoke.*;
import java.util.Arrays;
import java.util.List;

// Only for immutable values
public class Values {

    public static BsValue wrapValue(Object value) {
        return BsValue.wrap(value);
    }
    
    public static boolean isTrue(BsValue value) {
        return BoolType.INSTANCE.isTrue(value);
    }
    
    public static BsValue inlineIncrement(Variable variable, boolean increment, boolean incrementFirst) {
        BsValue valueBefore = variable.get();
        if (increment) {
            variable.set(Operators.plus(variable.get(), new IntegerValue(1)));
        } else {
            variable.set(Operators.minus(variable.get(), new IntegerValue(1)));
        }
        return incrementFirst ? variable.get() : valueBefore;
    }
    
    public static BsValue makePrimitiveValue(MethodHandles.Lookup lookup, String name, Class<?> type, int id) {
        return switch (id) {
            case 0 -> NoValue.INSTANCE;
            case 10 -> NullValue.NULL;
            case 11 -> NullValue.NOTHING;
            case 12 -> NullValue.UNDEFINED;
            case 13 -> NullValue.NADA;
            case 14 -> NullValue.EMPTY;
            case 20 -> BoolValue.FALSE;
            case 21 -> BoolValue.TRUE;
            default -> throw new IncompatibleClassChangeError();
        };
    }
    
    public static BsValue makeIntegerValue(MethodHandles.Lookup lookup, String name, Class<?> type, int value) {
        return new IntegerValue(value);
    }
    
    public static BsValue makeFloatingValue(MethodHandles.Lookup lookup, String name, Class<?> type, double value) {
        return new FloatingValue(value);
    }
    
    public static BsValue makeStringValue(MethodHandles.Lookup lookup, String name, Class<?> type, BsType stringType, String value) {
        if (!(stringType instanceof StringType st) || st == StringType.UTF256) {
            throw new IncompatibleClassChangeError(stringType.toString());
        } else {
            return new StringValue(st, value);
        }
    }

    // UTF-256 is unescaped at runtime so it can be stored in the constant pool more easily
    public static BsValue makeUtf256Value(MethodHandles.Lookup lookup, String name, Class<?> type, String value) {
        return new StringValue(StringType.UTF256, StringEscapeHelper.unescapeUtf256(value));
    }

    // UTF-256 is unescaped at runtime so it can be stored in the constant pool more easily
    // Every $ sign in template is replaced with an interpolation arg.
    // For literal $, use the string "$" as interpolation arg.
    public static CallSite makeUtf256ValueInterpolation(MethodHandles.Lookup lookup, String name, MethodType concatType, String template) throws NoSuchMethodException, IllegalAccessException {
        String[] splits = template.split("\\$", -1);
        List<int[]> unescapedConstantParts = Arrays.stream(splits).map(StringEscapeHelper::unescapeUtf256).toList();

        if (!concatType.returnType().isAssignableFrom(BsValue.class)) {
            throw new IncompatibleClassChangeError("makeUtf256ValueInterpolation should return a Value");
        }

        // Use our own lookup to find private method in this class
        MethodHandle doConcat = MethodHandles.lookup().findStatic(Utf256Joiner.class, "join", MethodType.methodType(BsValue.class, List.class, Object[].class));
        MethodHandle withConstants = MethodHandles.insertArguments(doConcat, 0, unescapedConstantParts);
        MethodHandle withPositionalArgs = withConstants.asCollector(Object[].class, concatType.parameterCount());

        MethodHandle[] argFilters = new MethodHandle[concatType.parameterCount()];
        for (int i = 0; i < argFilters.length; i++) {
            argFilters[i] = MethodUtil.wrapper(concatType.parameterType(i));
        }

        MethodHandle withWrappedPrimitives = MethodHandles.filterArguments(withPositionalArgs, 0, argFilters);

        // Primitives will now be wrapped. Everything else can be cast to object, explicit cast should always work.
        MethodHandle withCast = MethodHandles.explicitCastArguments(withWrappedPrimitives, concatType);

        return new ConstantCallSite(withCast);
    }
}
