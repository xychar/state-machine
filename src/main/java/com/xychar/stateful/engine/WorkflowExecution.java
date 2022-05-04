package com.xychar.stateful.engine;

/**
 * Implemented by WorkflowInstance.
 */
public interface WorkflowExecution<T> {
    /**
     * UUID to make each execution unique.
     */
    String getExecutionId();

    /**
     * Retrieve the workflow instance object.
     */
    T getWorkflowInstance();
}
