package bsx.compiler.jvm.override;

import bsx.load.LoadingContext;
import bsx.load.TypeHierarchy;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Stream;

public class TypeHierarchyResolver {
    
    private final LoadingContext context;
    private final Map<String, TypeHierarchy> data;
    
    public TypeHierarchyResolver(LoadingContext context, List<ClassNode> classes) {
        this.context = context;
        Map<String, TypeHierarchy> data = new HashMap<>();
        for (ClassNode node : classes) {
            data.put(node.name, TypeHierarchy.of(node));
        }
        this.data = Map.copyOf(data);
    }
    
    public TypeHierarchy get(String className) {
        if (this.data.containsKey(className)) {
            return this.data.get(className);
        } else {
            return this.context.getInheritance(className);
        }
    }
    
    public List<String> getLinearizedSuperClasses(String className) {
        if (Objects.equals(className, "java/lang/Object")) return List.of();
        List<String> classes = new ArrayList<>();
        List<String> interfaces = new ArrayList<>();
        TypeHierarchy hierarchy = null;
        do {
            hierarchy = this.get(hierarchy == null ? className : hierarchy.superClass());
            classes.add(hierarchy.superClass());
            for (String itf : hierarchy.interfaces()) {
                if (!interfaces.contains(itf) )interfaces.add(itf);
            }
        } while (!Objects.equals(hierarchy.superClass(), "java/lang/Object"));
        return Stream.concat(classes.stream(), interfaces.stream()).toList();
    }
    
    public List<Class<?>> getLoadedSuperClasses(String className) {
        return this.getLinearizedSuperClasses(className).stream().<Class<?>>flatMap(name -> {
            try {
                return Stream.of(Class.forName(name.replace("/", "."), false, this.context.loader()));
            } catch (ClassNotFoundException e) {
                return Stream.empty();
            }
        }).toList();
    }
}
