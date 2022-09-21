package bsx.load;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.List;

public record TypeHierarchy(String superClass, List<String> interfaces) {
    
    public static TypeHierarchy of(Class<?> cls) {
        Class<?> superClass = cls.getSuperclass();
        String superName = superClass == null ? "java/lang/Object" : superClass.getName().replace(".", "/");
        List<String> interfaces = Arrays.stream(cls.getInterfaces())
                .map(itf -> itf.getName().replace(".", "/"))
                .toList();
        return new TypeHierarchy(superName, interfaces);
    }
    
    public static TypeHierarchy of(ClassNode cls) {
        String superName = (cls.superName == null || (cls.access & Opcodes.ACC_INTERFACE) != 0) ? "java/lang/Object" : cls.superName;
        return new TypeHierarchy(superName, List.copyOf(cls.interfaces));
    }
}
