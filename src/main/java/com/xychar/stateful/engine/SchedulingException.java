package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public class SchedulingException extends Throwable {
    public Method currentMethod;

    public StepStateItem stepStateItem;

    public long waitingTime;

    public SchedulingException() {
        super("Workflow scheduling exception");
    }
}
