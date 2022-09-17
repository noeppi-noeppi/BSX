package bsx.util.string;

import bsx.BSX;
import bsx.resolution.InvokeAsInstanceMethod;
import bsx.resolution.SpecialInvoke;
import bsx.util.ValueHelper;

// Methods for the string type. For non-printable UTF-256 stings use Utf256Ops
// All methods from one class must be in the other as well
public class StringOps {
    
    @InvokeAsInstanceMethod
    public static String charAt(String string, int at) {
        return Character.toString(string.codePointAt(at));
    }

    @InvokeAsInstanceMethod
    public static String substr(String string, int off) {
        long length = string.codePoints().count();
        if (off < 0 || off > length) throw new StringIndexOutOfBoundsException(off);
        return ValueHelper.fromCodePoints(string.codePoints().skip(off));
    }
    
    @InvokeAsInstanceMethod
    public static String substr(String string, int off, int len) {
        long length = string.codePoints().count();
        if (off < 0 || off > length) throw new StringIndexOutOfBoundsException(off);
        if (len < 0 || off + len > length) throw new StringIndexOutOfBoundsException(len);
        return ValueHelper.fromCodePoints(string.codePoints().skip(off).limit(len));
    }
    
    @SpecialInvoke
    @InvokeAsInstanceMethod
    public static void EVALUATE(String string) {
        BSX.evaluate(string);
    }
}
