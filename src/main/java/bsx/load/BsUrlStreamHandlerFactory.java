package bsx.load;

import bsx.Bootstrap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class BsUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    private final Handler bsHandler;

    public BsUrlStreamHandlerFactory() {
        this.bsHandler = new Handler();
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return "bs".equalsIgnoreCase(protocol) ? this.bsHandler : null;
    }
    
    private static class Handler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new Connection(url);
        }
    }
    
    private static class Connection extends URLConnection {

        @Nullable
        private InputStream in;
        
        private Connection(URL url) {
            super(url);
            this.in = null;
        }

        @Override
        public void connect() throws IOException {
            synchronized (this) {
                if (this.in == null) {
                    String name = this.url.getPath();
                    name = name.replaceAll("/+", "/");
                    if (name.startsWith("/")) name = name.substring(1);
                    if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                    this.in = Bootstrap.context().getClass(name);
                }
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            this.connect();
            return this.in;
        }
    }
}
