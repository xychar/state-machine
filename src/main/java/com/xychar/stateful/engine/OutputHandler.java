package com.xychar.stateful.engine;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;

public interface OutputHandler {
    /**
     * Non-default methods for output property access.
     */
    @RuntimeType
    @BindingPriority(300)
    Object property(@This OutputProxy parent, @Origin Method method,
                    @AllArguments Object... args) throws Throwable;

}
