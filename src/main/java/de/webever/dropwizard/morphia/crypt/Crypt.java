package de.webever.dropwizard.morphia.crypt;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This field will be encrypted before it is persisted and encrypted after load.
 * This will only work on strings. Fields of other type will be ignored.
 * 
 * @author Richard Naeve
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Crypt {
}
