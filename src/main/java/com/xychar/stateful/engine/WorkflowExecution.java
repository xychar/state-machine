package com.xychar.stateful.engine;

public interface WorkflowExecution<T> {
    String getExecutionId();

    T getWorkflowInstance();
}
