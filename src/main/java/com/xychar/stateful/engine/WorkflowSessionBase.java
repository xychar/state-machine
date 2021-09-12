package com.xychar.stateful.engine;

public class WorkflowSessionBase<T> implements WorkflowSession<T> {

    public StepHandler handler;

    public Interceptor delegate;

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;

        return workflowInstance;
    }
}
