package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowStatus;

import java.lang.reflect.Method;
import java.time.Instant;

public class WorkflowData {
    public String executionId;
    public String workerName;
    public String sessionId;
    public String className;
    public String methodName;
    public Method stepMethod;
    public WorkflowStatus status;
    public Integer executions;
    public Instant startTime;
    public Instant endTime;
    public Instant lastRun;
    public Instant nextRun;
    public Object returnValue;
    public Throwable exception;
    public String configData;
}
