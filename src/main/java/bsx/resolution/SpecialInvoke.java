package bsx.resolution;

import java.lang.annotation.*;

// Make a no arg method behave as a field during resolution.
// Single arg methods act as field setters

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpecialInvoke {
    
}
