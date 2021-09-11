package com.xychar.stateful.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class WorkflowHandler implements InvocationHandler {
    private final WorkflowMetadata metadata;
    public final WorkflowSessionBase session;

    public WorkflowHandler(WorkflowMetadata metadata, WorkflowSessionBase session) {
        this.metadata = metadata;
        this.session = session;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
