package bs;

import bsx.BSX;
import bsx.BsValue;
import bsx.resolution.NoLookup;
import bsx.resolution.SpecialInvoke;
import bsx.util.StackTraceCleaner;
import bsx.value.ArrayValue;
import bsx.value.FloatingValue;
import bsx.value.StringValue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class Predef {
    
    @SpecialInvoke
    public static void HALT_AND_CATCH_FIRE() {
        BSX.UNSAFE.throwException(new Exception("HALT_AND_CATCH_FIRE"));
    }
    
    @NoLookup
    public static void echo(BsValue value) {
        if (value instanceof StringValue sv && sv.getPrintableString().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int[] utf256 = sv.getRaw().toArray();
            for (int i = 0; i + 7 < utf256.length; i += 8) {
                if (utf256[i] != 0 || utf256[i + 1] != 0 || utf256[i + 2] != 0 || utf256[i + 3] != 0
                        || utf256[i + 4] != 0 || utf256[i + 5] != 0 || utf256[i + 6] != 0
                        || (!Character.isBmpCodePoint(utf256[i + 7]) && !Character.isValidCodePoint(utf256[i + 7]))) {
                    sb.append(BSX.REPLACEMENT_CHAR);
                } else {
                    sb.appendCodePoint(utf256[i + 7]);
                }
            }
            System.out.print(sb);
        } else {
            System.out.print(value);
        }
        System.out.flush();
    }
    
    public static long len(BsValue value) {
        if (value instanceof ArrayValue av) {
            return av.length();
        } else if (value instanceof StringValue sv) {
            return sv.getRaw().count();
        } else {
            return 0;
        }
    }
    
    public static BsValue array(BsValue... elems) {
        return new ArrayValue(List.of(elems));
    }
    
    @NoLookup
    public static void doAnd(MethodHandle[] blocks) {
        if (blocks.length == 1) {
            BSX.invoke(blocks[0]);
        } else if (blocks.length > 1) {
            List<Thread> threads = new ArrayList<>();
            for (MethodHandle block : blocks) {
                threads.add(new Thread(() -> BSX.invokeWithErrorHandling(block)));
            }
            try {
                for (Thread thread : threads) thread.start();
                for (Thread thread : threads) thread.join();
            } catch (Throwable t) {
                StackTraceCleaner.process(t);
                t.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    public static MethodHandle BS_RESOLVE(String name, List<BsValue> args, boolean instance, boolean special) {
        if (special && !instance && BSX.PIE.equals(name) && args.isEmpty()) {
            // PIE (resolved explicitly from pie literal in compiler)
            return MethodHandles.constant(BsValue.class, FloatingValue.PIE);
        } else {
            return null;
        }
    }
}
