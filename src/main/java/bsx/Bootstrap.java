package bsx;

import bsx.load.BsClassLoader;
import bsx.load.BsUrlStreamHandlerFactory;
import bsx.load.LoadingContext;
import bsx.type.StringType;

import java.net.URL;
import java.nio.charset.Charset;

public class Bootstrap {
    
    private static boolean debug = false;
    private static LoadingContext context = null;
    
    public static void bootstrap(boolean debug) {
        Bootstrap.debug = debug;
        Bootstrap.context = new LoadingContext(new BsClassLoader("BSX", ClassLoader.getSystemClassLoader()));
        
        // Ensure that the unsafe works
        int unused = BSX.UNSAFE.addressSize();
        
        // Load charsets for string types
        for (StringType type : StringType.values()) {
            Charset charset = type.charset;
        }

        URL.setURLStreamHandlerFactory(new BsUrlStreamHandlerFactory());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean debug() {
        return debug;
    }
    
    public static LoadingContext context() {
        return context;
    }
}
