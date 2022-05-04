package com.xychar.stateful.engine;

public enum WorkflowStatus {
    /**
     * The initial state of a workflow.
     */
    CREATED,

    /**
     * The workflow is executing.
     */
    EXECUTING,

    /**
     * The workflow is finished successfully.
     */
    FINISHED,

    /**
     * The workflow is rolling back.
     */
    ROLLBACK;
}
