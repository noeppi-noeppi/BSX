package bsx.load;

import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class BsClassLoader extends ClassLoader {

    private final Map<String, URL> urls;
    
    public BsClassLoader(String name, ClassLoader parent) {
        super(name, parent);
        this.urls = new HashMap<>();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        
        String theName = name.replace(".", "/");
        URL url = this.urls.get(theName);
        if (url == null) throw new ClassNotFoundException(name);
        try (InputStream in = url.openStream()) {
            byte[] data = in.readAllBytes();
            return this.defineClass(name, data, 0, data.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Nullable
    protected URL findResource(String name) {
        if (name.endsWith(".class")) {
            String theName = name.substring(0, name.length() - 6);
            while (theName.contains("//")) theName = theName.replace("//", "/");
            if (theName.startsWith("/")) theName = theName.substring(1);
            return this.urls.get(theName);
        } else {
            return null;
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        URL url = this.findResource(name);
        return url == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(url));
    }

    public void addURL(String className, URL url) {
        this.urls.put(className, url);
    }

    public static URL classURL(Type type) {
        if (type.getSort() != Type.OBJECT) throw new IllegalArgumentException("Not an object type: " + type);
        try {
            return new URL("bs:" + type.getInternalName());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
