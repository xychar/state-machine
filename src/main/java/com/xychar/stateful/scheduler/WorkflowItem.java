package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowState;

import java.lang.reflect.Method;
import java.time.Instant;

public class WorkflowItem {
    public String executionId;
    public String workerName;
    public String sessionId;
    public String className;
    public String methodName;
    public Method stepMethod;
    public String stepKey;
    public WorkflowState state;
    public Integer executionTimes;
    public Instant startTime;
    public Instant endTime;
    public Instant nextRun;
    public Instant lastRun;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;
    public String configData;
}
