package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepState {
    public WorkflowHandler handler;
    public RetryState retrying;
    public StepStatus action;

    public String executionId;
    public Method stepMethod;
    public String stepKey;
    public StepStatus status;

    public String message;
    public String lastResult;
    public int executionTimes;

    public Instant startTime;
    public Instant endTime;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;

    public Instant currentRun;
    public Instant nextRun;
    public Integer userVarInt;
    public String userVarStr;
    public String userVarObj;
}
