package com.xychar.stateful.engine;

public enum WorkflowState {
    /**
     * The initial state of a workflow.
     */
    Created,

    /**
     * The workflow is executing.
     */
    Executing,

    /**
     * The workflow is finished successfully.
     */
    Finished,

    /**
     * The workflow is rolling back.
     */
    RollingBack,

    /**
     * The workflow is rolled back successfully.
     */
    RolledBack;
}
