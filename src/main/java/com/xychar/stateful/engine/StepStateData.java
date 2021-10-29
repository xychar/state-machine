package com.xychar.stateful.engine;

import java.time.Instant;

public class StepStateData {
    public StepState state;
    public Object returnValue;
    public Object[] parameters;
    public Throwable exception;
    public int executionTimes;
    public int maxExecutionTimes;
    public Instant startTime;
    public Instant endTime;
    public Instant currentRun;
    public Instant nextRun;
}
