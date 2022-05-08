package com.xychar.stateful.exception;

public class TimeOutException extends WorkflowException {
    public TimeOutException(String message) {
        super(message);
    }

    public TimeOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
