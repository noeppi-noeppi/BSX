package bsx.invoke;

import bsx.BsValue;
import bsx.variable.Variable;

import java.lang.invoke.*;

public class Language {

    public static Variable makeVariable(String name) {
        return new Variable(name);
    }
    
    public static Variable makeVariable(String name, BsValue initialValue) {
        Variable var = new Variable(name);
        var.set(initialValue);
        return var;
    }
    
    public static CallSite makeBlock(MethodHandles.Lookup lookup, String name, MethodType blockType, MethodHandle blockImplementation) throws NoSuchMethodException, IllegalAccessException {
        if (!blockType.returnType().isAssignableFrom(MethodHandle.class)) {
            throw new IncompatibleClassChangeError("makeBlock should return a MethodHandle");
        }
        if (blockType.parameterCount() != blockImplementation.type().parameterCount()) {
            throw new IncompatibleClassChangeError("Can't make block with mismatching amount of variables: block=" + blockImplementation.type().parameterCount() + ", args=" + blockType.parameterCount());
        }
        for (int i = 0; i < blockType.parameterCount(); i++) {
            if (blockType.parameterType(i) != blockImplementation.type().parameterType(i)) {
                throw new IncompatibleClassChangeError("Can't make block with mismatching variable type at " + i + " def=" + blockType.parameterType(i) + ", impl=" + blockImplementation.type().parameterType(i));
            }
        }
        if (blockType.hasPrimitives()) {
            throw new IncompatibleClassChangeError("Can't make block with primitives: " + blockType);
        }
        MethodHandle createBlock = MethodHandles.lookup().findStatic(Language.class, "createBlock", MethodType.methodType(MethodHandle.class, MethodHandle.class, Object[].class));
        MethodHandle withImpl = MethodHandles.insertArguments(createBlock, 0, blockImplementation);
        MethodHandle withPositionalArgs = withImpl.asCollector(Object[].class, blockType.parameterCount());
        
        // cast to Object always works as there are no primitives
        MethodHandle withCast = MethodHandles.explicitCastArguments(withPositionalArgs, blockType);
        
        return new ConstantCallSite(withCast);
    }
    
    private static MethodHandle createBlock(MethodHandle implementation, Object[] variables) {
        return MethodHandles.insertArguments(implementation, 0, variables);
    }
}