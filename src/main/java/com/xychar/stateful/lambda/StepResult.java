package com.xychar.stateful.lambda;

import com.xychar.stateful.engine.WorkflowStatus;

import java.time.Instant;

public class StepResult extends StepInput {
    public Integer waitingSeconds;
    public WorkflowStatus status;
    public Integer executions;

    public Instant startTime;
    public Instant endTime;
    public Instant lastRun;
    public Instant nextRun;

    public Object returnValue;
    public Throwable exception;
}
