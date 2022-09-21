package bsx.type;

import bsx.BsType;
import bsx.BsValue;
import bsx.resolution.Resolver;
import bsx.util.string.StringOps;
import bsx.util.string.Utf256Ops;
import bsx.value.NoValue;
import bsx.value.StringValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public enum StringType implements BsType {
    
    ASCII("ASCII", "'", "'", StandardCharsets.US_ASCII),
    ANSI("ANSI", "''", "''", StandardCharsets.ISO_8859_1),
    DBCS("DBCS", "\"", "\"", StandardCharsets.UTF_16LE),
    EBCDIC("EBCDIC", "\"\"", "\"\"", Charset.forName("IBM037")),
    UTF256("String", "«", "»", null);
    
    public final String name;
    private final String literalStart;
    private final String literalEnd;
    
    @Nullable
    public final Charset charset;

    StringType(String name, String literalStart, String literalEnd, @Nullable Charset charset) {
        this.name = name;
        this.literalStart = literalStart;
        this.literalEnd = literalEnd;
        this.charset = charset;
    }

    public String wrap(String string) {
        return this.literalStart + string + this.literalEnd;
    }
    
    @Override
    public BsValue cast(BsValue value) {
        if (value.getType() == NoValue.INSTANCE) {
            return new StringValue(this, "");
        } else if (value instanceof StringValue sv) {
            return sv.getType() == this ? sv : sv.copyWith(this);
        } else {
            return new StringValue(UTF256, value.toString()).copyWith(this);
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        if (instance && !special && name.equals("__invoke") && args.size() == 2) {
            return this.resolve("charAt", args, true, false);
        }
        if (instance && special && name.equals("bytes") && args.size() == 1) {
            return MethodHandles.publicLookup().findVirtual(StringValue.class, "getBytes", MethodType.methodType(byte[].class));
        }
        if (instance) {
            if (args.isEmpty() || !(args.get(0) instanceof StringValue sv)) return null;
            
            boolean hasAnyNonPrintableStringArgs = args.stream().skip(1)
                    .filter(v -> v instanceof StringValue)
                    .map(v -> (StringValue) v)
                    .anyMatch(v -> v.getPrintableString().isEmpty());

            // For strings that can be represented with regular java strings, resolve in StringOps
            // otherwise use Utf256Ops
            // assume that args need to be printable as well. If not, also fall back to Utf256Ops
            if (sv.getPrintableString().isPresent() && !hasAnyNonPrintableStringArgs) {
                MethodHandle result = Resolver.resolve(String.class, StringOps.class, name, args, true, special);
                if (result == null) return null;
                MethodHandle selfHandle = MethodHandles.filterArguments(result, 0, StringValue.findStringGetter());
                return this.fixResultStringType(selfHandle);
            } else {
                MethodHandle result = Resolver.resolve(int[].class, Utf256Ops.class, name, args, true, special);
                if (result == null) return null;
                return MethodHandles.filterArguments(result, 0, StringValue.findUtfGetter());
            }
        } else {
            return Resolver.resolve(StringValue.class, StringOps.class, name, args, false, special);
        }
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
    private MethodHandle fixResultStringType(MethodHandle handle) throws NoSuchMethodException, IllegalAccessException {
        // If the method from StringOps produces a new string or char it should have the same string type as this one.
        if (handle.type().returnType() == char.class) {
            MethodHandle charToString = MethodHandles.lookup().findStatic(Character.class, "toString", MethodType.methodType(String.class, char.class));
            return this.fixResultStringType(MethodHandles.filterReturnValue(handle, charToString));
        } else if (handle.type().returnType() == String.class) {
            MethodHandle newStringValue = MethodHandles.lookup().findConstructor(StringValue.class, MethodType.methodType(void.class, StringType.class, String.class));
            MethodHandle newStringValueWithCurrentType = MethodHandles.insertArguments(newStringValue, 0, this);
            return MethodHandles.filterReturnValue(handle, newStringValueWithCurrentType);
        } else if (handle.type().returnType() == String[].class) {
            MethodHandle newStringArray = MethodHandles.lookup().findStatic(StringType.class, "fixStringArray", MethodType.methodType(StringValue[].class, StringType.class, String[].class));
            MethodHandle newStringArrayWithCurrentType = MethodHandles.insertArguments(newStringArray, 0, this);
            return MethodHandles.filterReturnValue(handle, newStringArrayWithCurrentType);
        } else {
            return handle;
        }
    }
    
    private static StringValue[] fixStringArray(StringType type, String[] values) {
        return Arrays.stream(values).map(str -> new StringValue(type, str)).toArray(StringValue[]::new);
    }
}
