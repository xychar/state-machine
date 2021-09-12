package com.xychar.stateful.engine;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bind.annotation.DefaultMethod;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface Interceptor {

    Object invoke(Callable<?> superCall, Method method, Object[] args) throws Throwable;
}
