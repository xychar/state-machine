package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepState {
    public Method stepMethod;
    public StepStatus status;
    /**
     * Step status can be set in step execution.
     */
    public StepStatus result;
    public String message;
    public Instant startTime;
    public Instant endTime;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;
    public Object lastResult;
    public int executionTimes;
    public int maxAttempts;
    public Instant currentRun;
    public Instant nextRun;
    public Integer userVarInt;
    public String userVarStr;
    public String userVarObj;

}
