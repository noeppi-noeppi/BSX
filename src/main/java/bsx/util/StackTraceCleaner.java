package bsx.util;

import bsx.Bootstrap;

import java.util.Arrays;

public class StackTraceCleaner {
    
    public static void process(Throwable t) {
        if (!Bootstrap.debug()) {
            StackTraceElement[] trace = Arrays.stream(t.getStackTrace())
                    .filter(StackTraceCleaner::shouldKeep)
                    .toArray(StackTraceElement[]::new);
            t.setStackTrace(trace);
        }
    }
    
    @SuppressWarnings("RedundantIfStatement")
    private static boolean shouldKeep(StackTraceElement trace) {
        if (trace.getClassName().equals("bsx.BSX") && trace.getMethodName().startsWith("invoke")) return false;
        if (trace.getClassName().startsWith("bsx.invoke.")) return false;
        if (trace.getClassName().equals("java.lang.invoke.MethodHandle") && trace.getMethodName().equals("invokeWithArguments")) return false;
        return true;
    }
}
