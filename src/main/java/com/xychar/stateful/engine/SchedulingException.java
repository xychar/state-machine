package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public class SchedulingException extends Throwable {
    public Method currentMethod;

    public StepStateData stepStateData;

    public SchedulingException() {
        super("Workflow scheduling exception");
    }
}
