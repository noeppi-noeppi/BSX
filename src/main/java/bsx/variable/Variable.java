package bsx.variable;

import bsx.BsValue;
import bsx.value.NoValue;

import java.util.concurrent.atomic.AtomicReference;

public class Variable {
    
    private final String name;
    private final AtomicReference<BsValue> value;

    public Variable(String name) {
        this.name = name;
        this.value = new AtomicReference<>(NoValue.INSTANCE);
    }

    public String getName() {
        return this.name;
    }

    public BsValue get() {
        return this.value.get();
    }

    public void set(BsValue newValue) {
        this.value.set(newValue);
    }
}
