package com.xychar.stateful.engine;

import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Constructor;
import java.util.Map;

public class WorkflowMetadata<T> {
    public Class<?> workflowClass;

    public Constructor<? extends WorkflowInstance> workflowConstructor;

    public Map<Class<?>, Constructor<? extends OutputAccessor>> outputCreators;

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
