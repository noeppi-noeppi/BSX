package bsx.resolution;

import java.lang.annotation.*;

// Member can't be looked up

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoLookup {
    
}
