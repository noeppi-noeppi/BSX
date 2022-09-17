package bsx.value;

import bsx.BsType;
import bsx.BsValue;
import bsx.type.AnyType;
import bsx.type.StringType;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

public class StringValue implements BsValue {
    
    private final StringType type;
    private final int[] utf256;
    
    @Nullable
    private final String string;

    public StringValue(StringType type, String string) {
        this.type = type;
        this.utf256 = string.codePoints().flatMap(cp -> IntStream.of(0, 0, 0, 0, 0, 0, 0, cp)).toArray();
        this.string = string;
        this.validateString();
    }
    
    public StringValue(StringType type, int[] utf256) {
        this(type, utf256, 0, utf256.length);
    }
    
    public StringValue(StringType type, int[] utf256, int off, int len) {
        this.type = type;
        if (utf256.length % 8 != 0) throw new IllegalArgumentException("Invalid utf256 length");
        this.utf256 = new int[len];
        System.arraycopy(utf256, off, this.utf256, 0, len);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < utf256.length; i += 8) {
            if (this.utf256[i] != 0 || this.utf256[i + 1] != 0 || this.utf256[i + 2] != 0 || this.utf256[i + 3] != 0
                    ||this.utf256[i + 4] != 0 || this.utf256[i + 5] != 0 || this.utf256[i + 6] != 0
                    || (!Character.isBmpCodePoint(this.utf256[i + 7]) && !Character.isValidCodePoint(this.utf256[i + 7]))) {
                sb = null;
                break;
            }
            sb.appendCodePoint(this.utf256[i + 7]);
        }
        this.string = sb == null ? null : sb.toString();
        this.validateString();
    }
    
    public StringValue copyWith(StringType type) {
        try {
            return new StringValue(type, this.utf256);
        } catch (IllegalArgumentException e) {
            ClassCastException cce = new ClassCastException("Failed to cast invalid string for " + type.name);
            cce.initCause(e);
            throw cce;
        }
    }
    
    public Optional<String> getPrintableString() {
        return Optional.ofNullable(this.string);
    }

    public IntStream getRaw() {
        return Arrays.stream(this.utf256);
    }

    public byte[] getBytes() {
        if (this.string != null && this.type.charset != null) {
            ByteBuffer buffer = this.type.charset.encode(this.string);
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            return data;
        } else if (this.type == StringType.UTF256) {
            byte[] data = new byte[this.utf256.length * 4];
            for (int i = 0; i < this.utf256.length; i++) {
                data[4 * i] = (byte) ((this.utf256[i] >>> 24) & 0xFF);
                data[(4 * i) + 1] = (byte) ((this.utf256[i] >>> 16) & 0xFF);
                data[(4 * i) + 2] = (byte) ((this.utf256[i] >>> 8) & 0xFF);
                data[(4 * i) + 3] = (byte) (this.utf256[i] & 0xFF);
            }
            return data;
        } else {
            throw new IllegalStateException("Can't get string bytes of type " + this.type);
        }
    }
    
    private void validateString() {
        // null means utf256
        if (this.type.charset != null) {
            if (this.string == null) {
                throw new IllegalArgumentException("No string representation for " + this.type.name() + " string");
            }
            if (!this.type.charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .canEncode(this.string)) {
                throw new IllegalArgumentException("Invalid " + this.type.name() + " string: " + this.string);
            }
        }
    }

    @Override
    public StringType getType() {
        return this.type;
    }

    @Override
    public boolean matchesJava(Class<?> cls) {
        return cls == BsValue.class || (cls == String.class && this.string != null) || (cls == Character.class && this.string != null && this.string.length() == 1);
    }

    @Override
    public <T> T asJava(Class<T> cls) {
        if (cls == String.class) {
            //noinspection unchecked
            return (T) this.toString();
        } else if (cls == Character.class && this.string != null && this.string.length() == 1) {
            //noinspection unchecked
            return (T) Character.valueOf(this.string.charAt(0));
        } else {
            return BsValue.super.asJava(cls);
        }
    }
    
    @Override
    public boolean isOf(BsType type) {
        if (type == AnyType.INSTANCE) {
            return true;
        } else if (this.type == StringType.ASCII) {
            return type == StringType.ASCII || type == StringType.ANSI || type == StringType.DBCS || type == StringType.UTF256;
        } else {
            return this.type == type;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.utf256);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringValue sv) {
            return this.type == sv.type && Arrays.equals(this.utf256, sv.utf256);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.string != null) {
            return this.string;
        } else {
            throw new IllegalStateException("Unprintable string. Device is too weak for UTF-256");
        }
    }
    
    public static StringValue concatWith(StringType type, StringValue first, StringValue second) {
        int[] utf256 = new int[first.utf256.length + second.utf256.length];
        System.arraycopy(first.utf256, 0, utf256, 0, first.utf256.length);
        System.arraycopy(second.utf256, 0, utf256, first.utf256.length, second.utf256.length);
        return new StringValue(type, utf256);
    }
    
    public static boolean contentEquals(StringValue first, StringValue second) {
        return Arrays.equals(first.utf256, second.utf256);
    }
    
    // Method handles for private fields. Use with caution
    
    public static MethodHandle findStringGetter() throws NoSuchFieldException, IllegalAccessException {
        return MethodHandles.lookup().findGetter(StringValue.class, "string", String.class);
    }
    
    public static MethodHandle findUtfGetter() throws NoSuchFieldException, IllegalAccessException {
        return MethodHandles.lookup().findGetter(StringValue.class, "utf256", int[].class);
    }
}
