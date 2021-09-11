package com.xychar.stateful.engine;

public class WorkflowSessionBase<T> implements WorkflowSession<T> {

    public WorkflowHandler handler;

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;

        return workflowInstance;
    }
}
