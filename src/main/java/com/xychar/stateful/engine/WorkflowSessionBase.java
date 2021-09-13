package com.xychar.stateful.engine;

public class WorkflowSessionBase<T> implements WorkflowSession<T> {

    public Interceptor handler;

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;

        return workflowInstance;
    }
}
