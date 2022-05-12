package com.xychar.stateful.container;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bind.annotation.DefaultMethod;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface ContainerHandler {
    /**
     * Interface default methods for step definition.
     */
    @RuntimeType
    @BindingPriority(500)
    Object interceptProperty(@This ContainerProxy parent, @Origin Method method,
                             @DefaultMethod Method defaultMethod, @DefaultCall Callable<?> invocation,
                             @FieldProxy GetterAndSetter field, @AllArguments Object... args) throws Throwable;

    /**
     * Non-default methods for service and dependency injections.
     */
    @RuntimeType
    @BindingPriority(300)
    Object interceptProperty(@This ContainerProxy instance, @Origin Method method,
                             @AllArguments Object... args) throws Throwable;

}
