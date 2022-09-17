package bsx.resolution;

import java.lang.annotation.*;

// Invoke a static method as instance method.
// Will only be found if first param is the same type as lookup class

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeAsInstanceMethod {
    
}
