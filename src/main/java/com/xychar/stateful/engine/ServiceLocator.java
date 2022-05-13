package com.xychar.stateful.engine;

public interface ServiceLocator {
    <T> T lookup(Class<T> serviceClazz, String name);
}
