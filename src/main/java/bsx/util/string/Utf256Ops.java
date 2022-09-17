package bsx.util.string;

import bsx.resolution.InvokeAsInstanceMethod;
import bsx.resolution.SpecialInvoke;
import bsx.type.StringType;
import bsx.value.StringValue;

// UTF-256 strings that can be represented as java strings will also use StringOps
public class Utf256Ops {

    @InvokeAsInstanceMethod
    public static StringValue charAt(int[] utf256, int at) {
        int idx = at * 8;
        if (idx < 0 || idx + 7 >= utf256.length) {
            throw new StringIndexOutOfBoundsException(at);
        } else {
            int[] chr = new int[8];
            System.arraycopy(utf256, idx, chr, 0, 8);
            return new StringValue(StringType.UTF256, chr);
        }
    }

    @InvokeAsInstanceMethod
    public static StringValue substr(int[] utf256, int off) {
        long length = utf256.length / 8;
        if (off < 0 || off > length) throw new StringIndexOutOfBoundsException(off);
        int len = utf256.length - off;
        int[] data = new int[8 * len];
        System.arraycopy(utf256, 8 * off, data, 0, 8 * len);
        return new StringValue(StringType.UTF256, data);
    }

    @InvokeAsInstanceMethod
    public static StringValue substr(int[] utf256, int off, int len) {
        long length = utf256.length / 8;
        if (off < 0 || off > length) throw new StringIndexOutOfBoundsException(off);
        if (len < 0 || off + len > length) throw new StringIndexOutOfBoundsException(len);
        int[] data = new int[8 * len];
        System.arraycopy(utf256, 8 * off, data, 0, 8 * len);
        return new StringValue(StringType.UTF256, data);
    }

    @SpecialInvoke
    @InvokeAsInstanceMethod
    public static void EVALUATE(int[] utf256) {
        throw new IllegalArgumentException("Can't EVALUATE non-printable string.");
    }
}
