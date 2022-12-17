package bs;

import bsx.BSX;
import bsx.BsValue;
import bsx.resolution.NoLookup;
import bsx.resolution.Singleton;
import bsx.resolution.SpecialInvoke;
import bsx.util.StackTraceCleaner;
import bsx.value.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Singleton
public final class Predef {
    
    @SpecialInvoke
    public static void HALT_AND_CATCH_FIRE() {
        BSX.UNSAFE.throwException(new Exception("HALT_AND_CATCH_FIRE"));
    }
    
    @NoLookup
    public static void echo(BsValue value) {
        System.out.print(BSX.getPrintableString(value));
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
    
    public static BsValue in_array(BsValue value, BsValue array) {
        if (array instanceof ArrayValue av) {
            return BoolValue.of(av.values().stream().anyMatch(v -> Objects.equals(value, v)));
        } else {
            return NullValue.EMPTY;
        }
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
