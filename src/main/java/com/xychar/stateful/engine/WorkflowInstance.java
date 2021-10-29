package com.xychar.stateful.engine;

public class WorkflowInstance<T> implements WorkflowExecution<T> {

    public String executionId;

    public StepHandler handler;

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;

        return workflowInstance;
    }
}
