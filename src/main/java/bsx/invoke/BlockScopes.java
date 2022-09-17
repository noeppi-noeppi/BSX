package bsx.invoke;

import bsx.BSX;
import bsx.compiler.lvt.Scope;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

// Needed for EVALUATE. The compiler adds calls to this class that keep BSX.CURRENT_SCOPE up to date
public class BlockScopes {

    public static Scope blockScope(MethodHandles.Lookup lookup, String name, Class<?> type, int offset, String varNames, String deletedVars) {
        List<String> variables = varNames.isEmpty() ? List.of() : List.of(varNames.split("\0", -1));
        Set<String> deleted = varNames.isEmpty() ? Set.of() : Set.of(deletedVars.split("\0", -1));
        return new Scope() {
            
            @Override
            public int offset() {
                return offset;
            }

            @Override
            public List<String> availableVariables() {
                return variables;
            }

            @Override
            public Set<String> deletedVariables() {
                return deleted;
            }
        };
    }
    
    public static void setEvaluationSnapshot(Scope scope, Object[] variables) {
        BSX.setEvaluationSnapshot(scope, variables);
    }
}
