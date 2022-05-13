package com.xychar.stateful.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

public class ContainerMetadata<T> implements ContainerHandler {
    public Class<T> containerClass;
    public Class<? extends ContainerProxy> containerProxyClass;

    public Map<String, Integer> methodNames;
    Constructor<? extends ContainerProxy> containerConstructor;

    public T newInstance() {
        try {
            ContainerProxy instance = containerConstructor.newInstance();
            instance.fields = new Object[methodNames.size()];
            instance.handler = this;
            @SuppressWarnings("unchecked")
            T result = (T) instance;
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create container instance", e);
        }
    }

    @Override
    public Object interceptProperty(ContainerProxy parent, Method method,
                                    Method defaultMethod, Callable<?> invocation,
                                    int methodIndex, Object... args) throws Throwable {
        if (methodIndex >= 0 && methodIndex < parent.fields.length) {
            Object fieldValue = parent.fields[methodIndex];
            if (fieldValue == null) {
                fieldValue = invocation.call();
                parent.fields[methodIndex] = fieldValue;
            }

            return fieldValue;
        } else {
            throw new ContainerException("Method index out of range, method: " + method.getName());
        }
    }

    @Override
    public Object interceptProperty(ContainerProxy instance, Method method,
                                    Object... args) throws Throwable {
        return null;
    }
}
