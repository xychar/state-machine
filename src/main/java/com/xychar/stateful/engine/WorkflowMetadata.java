package com.xychar.stateful.engine;

import java.lang.reflect.Constructor;

public class WorkflowMetadata<T> {
    public Class<?> workflowClass;

    public Class<?> workflowProxyClass;

    public Constructor<?> workflowConstructor;

    public WorkflowInstance<T> newInstance() {
        try {
            @SuppressWarnings("unchecked")
            WorkflowInstance<T> instance = (WorkflowInstance<T>) workflowConstructor.newInstance();
            return instance;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow instance", e);
        }
    }
}
