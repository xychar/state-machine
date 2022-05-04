package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public class OutputProxy<T> {
    public Class<T> outputClass;
    public Method outputMethod;
    public OutputHandler handler;
}
