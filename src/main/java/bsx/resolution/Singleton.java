package bsx.resolution;

// Singleton classes can't resolve constructors, instance and parent properties.
// Must be final

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Singleton {
    
}
