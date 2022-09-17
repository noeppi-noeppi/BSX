package bsx.load;

import bsx.Bootstrap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoadingContext {
    
    private final Object LOCK = new Object();
    private final BsClassLoader loader;
    private final Map<String, String> classInheritance;
    private final Map<String, byte[]> classData;
    private int nextRuntimeId;
    private Path dumpTarget = null;
    
    public LoadingContext(BsClassLoader loader) {
        this.loader = loader;
        this.classInheritance = new HashMap<>();
        this.classData = new HashMap<>();
    }

    public BsClassLoader loader() {
        return this.loader;
    }

    public MethodHandle registerAnLoadMain(ClassNode mainNode) {
        String className = "bs/runtime/Main$" + (this.nextRuntimeId++);
        Remapper remapper = new Remapper() {
            
            @Override
            public String map(String internalName) {
                return mainNode.name.equals(internalName) ? className : internalName;
            }
        };
        
        ClassNode node = new ClassNode();
        ClassVisitor visitor = new ClassRemapper(node, remapper);
        mainNode.accept(visitor);
        
        node.access = Opcodes.ACC_PUBLIC;
        synchronized (this.LOCK) {
            node.name = className;
        }
        
        if (!Type.getType(Object.class).getInternalName().equals(node.superName)) {
            throw new IllegalArgumentException("Main code must extend Object");
        }

        MethodNode main = null;
        for (MethodNode method : node.methods) {
            if ("main".equals(method.name)) {
                if (main != null) {
                    throw new IllegalArgumentException("Multiple main methods in main code class");
                }
                main = method;
            }
        }
        if (main == null) {
            throw new IllegalArgumentException("No main method in main code class");
        }
        
        this.registerClass(node);
        this.loadClass(node);
        try {
            Class<?> loaded = Class.forName(node.name.replace("/", "."), true, this.loader);
            MethodType methodType = MethodType.fromMethodDescriptorString(main.desc, this.loader);
            return MethodHandles.lookup().findStatic(loaded, main.name, methodType);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Main code not properly loaded", e);
        }
    }
    
    public void registerClass(ClassNode cls) {
        synchronized (this.LOCK) {
            if (this.classInheritance.containsKey(cls.name)) {
                throw new IllegalStateException("Named class registered twice: " + cls.name);
            }
            this.classInheritance.put(cls.name, superNameFor(cls));
        }
    }
    
    public void loadClass(ClassNode cls) {
        ClassWriter cw = new BsClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cls.accept(cw);
        byte[] data = cw.toByteArray();
        if (this.dumpTarget != null) {
            try {
                Path path = this.dumpTarget.resolve(cls.name + ".class");
                Files.createDirectories(path.getParent());
                Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        synchronized (this.LOCK) {
            if (!this.classInheritance.containsKey(cls.name)) {
                throw new IllegalStateException("Class needs to be registered before it can be loaded: " + cls.name);
            }
            if (this.classData.containsKey(cls.name)) {
                throw new IllegalStateException("Class loaded twice: " + cls.name);
            }
            if (!Objects.equals(this.classInheritance.get(cls.name), superNameFor(cls))) {
                throw new IllegalStateException("Can't change super class during classloading: " + cls.name + " (was " + this.classInheritance.get(cls.name) + ", now is " + superNameFor(cls) + ")");
            }
            this.classData.put(cls.name, data);
            this.loader.addURL(cls.name, BsClassLoader.classURL(Type.getObjectType(cls.name)));
        }
    }
    
    public String getSuperClass(String cls) {
        if ("java/lang/Object".equals(cls)) return "java/lang/Object";
        synchronized (this.LOCK) {
            if (this.classInheritance.containsKey(cls)) {
                return this.classInheritance.get(cls);
            }
        }
        try {
            Class<?> loadedClass = Class.forName(cls.replace("/", "."), false, ClassLoader.getSystemClassLoader());
            Class<?> superClass = loadedClass.getSuperclass();
            return superClass == null ? "java/lang/Object" : superClass.getName().replace(".", "/");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Incomplete type hierarchy: Superclass missing for " + cls, e);
        }
    }
    
    public InputStream getClass(String cls) throws IOException {
        synchronized (this.LOCK) {
            if (this.classData.containsKey(cls)) {
                return new ByteArrayInputStream(this.classData.get(cls));
            } else {
                throw new FileNotFoundException("bs:" + cls);
            }
        }
    }
    
    public void dumpTo(Path basePath) throws IOException {
        if (!Bootstrap.debug()) throw new IllegalStateException("Can only dump in debug mode");
        if (this.dumpTarget != null) throw new IllegalStateException("Can only dump to a single target");
        basePath = basePath.toAbsolutePath().normalize();
        Files.createDirectories(basePath);
        this.dumpTarget = basePath;
        for (String cls : this.classData.keySet()) {
            Path path = basePath.resolve(cls + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, this.classData.get(cls), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    private static String superNameFor(ClassNode cls) {
        if (cls.superName == null || (cls.access & Opcodes.ACC_INTERFACE) != 0) {
            return "java/lang/Object";
        } else {
            return cls.superName;
        }
    }
}
