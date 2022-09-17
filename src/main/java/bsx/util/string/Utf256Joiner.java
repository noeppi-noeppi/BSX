package bsx.util.string;

import bsx.BsValue;
import bsx.type.StringType;
import bsx.value.StringValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Utf256Joiner {

    // Used as MethodHandle from Values.java
    // joins the constant parts with the args in between.
    public static BsValue join(List<int[]> constantParts, Object[] args) {
        List<int[]> parts = new ArrayList<>();
        for (int i = 0; i < constantParts.size(); i++) {
            parts.add(constantParts.get(i));
            if (i < args.length) {
                if (args[i] instanceof StringValue sv) {
                    // To support interpolating utf256 strings into other utf256 strings
                    parts.add(sv.getRaw().toArray());
                } else {
                    String str = args[i].toString();
                    int[] utf256 = str.codePoints().flatMap(cp -> IntStream.of(0, 0, 0, 0, 0, 0, 0, cp)).toArray();
                    parts.add(utf256);
                }
            }
        }
        int totalLength = 0;
        for (int[] part : parts) {
            totalLength += part.length;
        }
        int[] result = new int[totalLength];
        int currentIdx = 0;
        for (int[] part : parts) {
            System.arraycopy(part, 0, result, currentIdx, part.length);
            currentIdx += part.length;
        }
        return new StringValue(StringType.UTF256, result);
    }
}
