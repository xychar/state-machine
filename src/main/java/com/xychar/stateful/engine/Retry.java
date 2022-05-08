package com.xychar.stateful.engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    int maxAttempts() default 3;

    int firstInterval() default 0;

    int intervalSeconds() default 30;

    double backoffRate() default 1.0;

    int timeoutSeconds() default 300;

    Class<? extends Throwable>[] exceptions() default {};

    boolean succeedAfterRetrying() default false;
}
