package com.xychar.stateful.engine;

public enum StepStatus {
    /**
     * The initial state of a step.
     * <p>
     * Non-workflow-step function always return Undefined.
     */
    CREATED,

    /**
     * The step is called asynchronously, but not really started.
     */
    SCHEDULED,

    /**
     * The step function is executing.
     */
    EXECUTING,

    /**
     * The step is retrying after first failure.
     */
    RETRYING,

    /**
     * The external step is waiting for external events.
     */
    WAITING,

    /**
     * The step is failed, the whole workflow will be stopped.
     */
    FAILED,

    /**
     * The step is finished successfully.
     */
    DONE;
}
