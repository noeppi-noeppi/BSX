package bsx.compiler.jvm.statement;


import org.objectweb.asm.tree.LabelNode;

import java.util.HashMap;
import java.util.Map;

public class Labels {
    
    private final Map<Long, LabelNode> labels;
    private boolean frozen;

    public Labels() {
        this.labels = new HashMap<>();
        this.frozen = false;
    }
    
    public Labels(Labels parent) {
        this.labels = new HashMap<>(parent.labels);
        this.frozen = false;
    }
    
    public void addLabel(long label) {
        if (this.frozen) throw new IllegalStateException("Labels have already been frozen");
        LabelNode node = new LabelNode();
        node.getLabel(); // Immediately resolve label
        this.labels.put(label, node);
    }
    
    public void freeze() {
        this.frozen = true;
    }
    
    public LabelNode getLabel(long label) {
        if (!this.labels.containsKey(label)) {
            throw new IllegalStateException("Label not found or not reachable from here: " + label);
        }
        return this.labels.get(label);
    }
}
