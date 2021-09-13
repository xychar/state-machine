package com.xychar.stateful.engine;

public enum StepState {
    /**
     * The initial state of a step.
     * <p>
     * Non-workflow-step function always return Undefined.
     */
    Undefined,

    /**
     * The step is called asynchronously, but not really started.
     */
    Scheduled,

    /**
     * The step function is executing.
     */
    Executing,

    /**
     * The step is retrying after first failure.
     */
    Retrying,

    /**
     * The external step is waiting for external events.
     */
    Waiting,

    /**
     * The step is failed, the whole workflow will be stopped.
     */
    Failed,

    /**
     * The step is finished successfully.
     */
    Done,

    ;
}
