package bsx.load;

import bsx.Bootstrap;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.List;

public class BsClassWriter extends ClassWriter {

    public BsClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        List<String> hierarchy1 = this.getHierarchy(type1);
        List<String> hierarchy2 = this.getHierarchy(type2);
        for (String type : hierarchy1) {
            if (hierarchy2.contains(type)) return type;
        }
        return "java/lang/Object";
    }
    
    private List<String> getHierarchy(String type) {
        List<String> list = new ArrayList<>();
        String current = type;
        do {
            list.add(current);
            current = Bootstrap.context().getInheritance(current).superClass();
        } while (!"java/lang/Object".equals(current));
        return list;
    }
}
