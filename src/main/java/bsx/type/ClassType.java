package bsx.type;

import bs.Predef;
import bsx.BsType;
import bsx.BsValue;
import bsx.resolution.Resolver;
import bsx.util.MethodUtil;
import bsx.value.NoValue;
import bsx.value.ObjectValue;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Objects;

public final class ClassType implements BsType {

    public static final ClassType PREDEF = new ClassType(Predef.class);
    
    private final String typeName;
    private Class<?> cls;

    public ClassType(String typeName) {
        this.typeName = typeName;
        this.cls = null;
    }
    
    public ClassType(Class<?> cls) {
        if (cls.isPrimitive() || cls.isArray() || cls == String.class) {
            throw new IllegalArgumentException();
        }
        this.typeName = cls.getName();
        this.cls = cls;
    }
    
    public static BsType makeCompileStatic(String sourceName) {
        String nameWithPackage = sourceName.contains(".") ? sourceName : "bs.lang." + sourceName;
        for (String part : nameWithPackage.split("\\.")) {
            if (part.isEmpty() || !Character.isJavaIdentifierStart(part.charAt(0))
                    || part.substring(1).chars().anyMatch(chr -> !Character.isJavaIdentifierPart(chr))) {
                throw new IllegalArgumentException("Invalid class name: " + sourceName);
            }
        }
        if (nameWithPackage.startsWith("bsx.")) {
            throw new IllegalStateException("Invalid syntax: " + sourceName);
        }
        return switch (nameWithPackage) {
            case "java.lang.String" -> StringType.UTF256;
            case "java.lang.Boolean" -> BoolType.INSTANCE;
            case "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long" -> IntegerType.INSTANCE;
            case "java.lang.Float", "java.lang.Double" -> FloatType.INSTANCE;
            case "java.lang.Void" -> NoValue.INSTANCE;
            default -> new ClassType(nameWithPackage);
        };
    }
    
    public String typeName() {
        return this.typeName;
    }
    
    public Class<?> cls() {
        if (this.cls == null) {
            try {
                this.cls = Class.forName(this.typeName);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to resolve compile-static class type");
            }
        }
        return this.cls;
    }
    
    @Override
    public BsValue cast(BsValue value) {
        if (value instanceof ObjectValue ov && this.cls().isAssignableFrom(ov.value.getClass())) {
            return value;
        } else {
            throw new ClassCastException(value + " cannot be cast to " + this);
        }
    }

    @Nullable
    @Override
    public MethodHandle resolve(String name, List<BsValue> args, boolean instance, boolean special) throws ReflectiveOperationException {
        if (!instance || (!args.isEmpty() && args.get(0) instanceof ObjectValue ov && this.cls().isAssignableFrom(ov.value.getClass()))) {
            MethodHandle handle = Resolver.resolve(this.cls(), name, args, instance, special);
            if (instance) {
                return MethodUtil.boundThis(handle, ((ObjectValue) args.get(0)).value);
            } else {
                return handle;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassType ct) {
            return Objects.equals(this.typeName, ct.typeName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.typeName.hashCode();
    }

    @Override
    public String toString() {
        return this.typeName.startsWith("bs.lang.") ? this.typeName.substring(8) : this.typeName;
    }
}
