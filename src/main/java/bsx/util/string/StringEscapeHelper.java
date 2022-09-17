package bsx.util.string;

import org.apache.commons.text.translate.*;

import java.math.BigInteger;
import java.util.Map;

public class StringEscapeHelper {
    
    private static final BigInteger BIT32_MASK = new BigInteger("FFFFFFFF", 16);
    
    private static final CharSequenceTranslator UNESCAPE_BS;
    static {
        UNESCAPE_BS = new AggregateTranslator(
                new OctalUnescaper(),
                new UnicodeUnescaper(),
                new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_UNESCAPE),
                new LookupTranslator(Map.of(
                        "\\\\", "\\",
                        "\\\"", "\"",
                        "\\'", "'",
                        "\\«", "«",
                        "\\»", "»",
                        "\\", ""
                ))
        );
    }
    
    public static String unescape(String string) {
        return UNESCAPE_BS.translate(string);
    }
    
    // Escape code \x followed by 64 hex digits to get any UTF-256 value
    public static int[] unescapeUtf256(String string) {
        int[] codePoints = string.codePoints().toArray();
        StringBuilder currentUtf8Part = new StringBuilder();
        
        // Unescaping won't make the string longer, so codePoints.length * 8 works as a maximum size
        int[] result = new int[codePoints.length * 8];
        int resultIdx = 0;
        for (int i = 0; i < codePoints.length; i++) {
            if (codePoints[i] == '\\' && i + 1 < codePoints.length && codePoints[i + 1] == 'x' && i + 65 < codePoints.length) {
                int[] pending = unescape(currentUtf8Part.toString()).codePoints().toArray();
                for (int codePoint : pending) {
                    // Everything unwritten is 0 in result anyway. Just initialise the last bit.
                    result[resultIdx + 7] = codePoint;
                    resultIdx += 8;
                }
                currentUtf8Part = new StringBuilder();
                
                String hexNum = new String(codePoints, i + 2, 64);
                i += 65; // 66th char is skipped by loop end.
                try {
                    BigInteger num = new BigInteger(hexNum, 16);
                    result[resultIdx] = num.shiftRight(224).or(BIT32_MASK).intValue();
                    result[resultIdx + 1] = num.shiftRight(192).or(BIT32_MASK).intValue();
                    result[resultIdx + 2] = num.shiftRight(160).or(BIT32_MASK).intValue();
                    result[resultIdx + 3] = num.shiftRight(128).or(BIT32_MASK).intValue();
                    result[resultIdx + 4] = num.shiftRight(96).or(BIT32_MASK).intValue();
                    result[resultIdx + 5] = num.shiftRight(64).or(BIT32_MASK).intValue();
                    result[resultIdx + 6] = num.shiftRight(32).or(BIT32_MASK).intValue();
                    result[resultIdx + 7] = num.or(BIT32_MASK).intValue();
                    resultIdx += 8;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid escape", e);
                }
            } else {
                currentUtf8Part.appendCodePoint(codePoints[i]);
            }
        }
        int[] pending = unescape(currentUtf8Part.toString()).codePoints().toArray();
        for (int codePoint : pending) {
            // Everything unwritten is 0 in result anyway. Just initialise the last bit.
            result[resultIdx + 7] = codePoint;
            resultIdx += 8;
        }
        int[] resultTruncated = new int[resultIdx];
        System.arraycopy(result, 0, resultTruncated, 0, resultIdx);
        return resultTruncated;
    }
}
