package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface Interceptor {

    Object invoke(Callable<?> superCall, Method method, Object[] args) throws Throwable;
}
