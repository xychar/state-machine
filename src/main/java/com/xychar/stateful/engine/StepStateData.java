package com.xychar.stateful.engine;

import java.time.Instant;

public class StepStateData {
    public StepState state;
    public String returnValue;
    public String parameters;
    public String exception;
    public int retriedTimes;
    public int maxRetryTimes;
    public Instant nextRun;
}
