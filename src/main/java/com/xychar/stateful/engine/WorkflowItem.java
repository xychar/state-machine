package com.xychar.stateful.engine;

import java.time.Instant;

public class WorkflowItem {
    public String sessionId;
    public String className;
    public String methodName;
    public WorkflowState state;
    public Integer executionTimes;
    public Instant startTime;
    public Instant endTime;
    public Instant nextRun;
    public Instant lastRun;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;
}
