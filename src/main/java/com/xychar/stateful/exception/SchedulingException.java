package com.xychar.stateful.exception;

import com.xychar.stateful.engine.StepState;

import java.lang.reflect.Method;

public class SchedulingException extends Throwable {
    public Exception lastException;
    public Method currentMethod;
    public StepState stepState;
    public long waitingTime;

    public SchedulingException() {
        super("Workflow scheduling exception");
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
