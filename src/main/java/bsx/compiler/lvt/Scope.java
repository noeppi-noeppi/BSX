package bsx.compiler.lvt;

import java.util.List;
import java.util.Set;

public interface Scope {
    
    Scope EMPTY = new Scope() {
        
        @Override
        public int offset() {
            return 0;
        }

        @Override
        public List<String> availableVariables() {
            return List.of();
        }

        @Override
        public Set<String> deletedVariables() {
            return Set.of();
        }
    };
    
    int offset();
    List<String> availableVariables();
    Set<String> deletedVariables();
}
