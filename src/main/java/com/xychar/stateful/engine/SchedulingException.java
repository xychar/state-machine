package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.time.Duration;

public class SchedulingException extends Throwable {
    public Method currentMethod;

    public StepStateData stepStateData;

    public long waitingTime;

    public SchedulingException() {
        super("Workflow scheduling exception");
    }
}
