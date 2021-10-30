package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepStateData {
    public StepState state;
    public StepState result;
    public String message;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;
    public int executionTimes;
    public int maxRetryTimes;
    public Instant startTime;
    public Instant endTime;
    public Instant currentRun;
    public Instant nextRun;
    public Method stepMethod;
}
