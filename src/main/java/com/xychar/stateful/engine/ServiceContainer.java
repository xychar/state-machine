package com.xychar.stateful.engine;

public interface ServiceContainer {
    <T> T lookupService(Class<T> serviceClazz, String name);
}
