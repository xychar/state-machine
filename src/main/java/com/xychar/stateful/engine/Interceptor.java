package com.xychar.stateful.engine;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface Interceptor {

    @RuntimeType
    public Object intercept(@This WorkflowSessionBase<?> session, @SuperCall Callable<?> superCall,
                            @Origin Method method, @AllArguments Object... args) throws Throwable;
}
