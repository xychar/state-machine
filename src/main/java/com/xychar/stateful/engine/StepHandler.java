package com.xychar.stateful.engine;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface StepHandler {

    @RuntimeType
    @BindingPriority(300)
    Object intercept(@This WorkflowInstance<?> instance, @SuperCall Callable<?> invocation,
                     @Origin Method method, @StepKeyArgs String stepKeyArgs,
                     @AllArguments Object... args) throws Throwable;

    /**
     * Non-default methods for service and dependency injections.
     */
    @RuntimeType
    @BindingPriority(500)
    Object intercept(@This WorkflowInstance<?> instance,
                     @Origin Method method, @StepKeyArgs String stepKeyArgs,
                     @AllArguments Object... args) throws Throwable;

    /**
     * Reserved method name.
     */
    @BindingPriority(100)
    void sleep(long milliseconds) throws Throwable;

    String getExecutionId();
}
