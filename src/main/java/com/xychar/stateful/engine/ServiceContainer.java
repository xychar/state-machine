package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface ServiceContainer {
    <T> T lookupService(Class<T> serviceClazz, String name);
}
