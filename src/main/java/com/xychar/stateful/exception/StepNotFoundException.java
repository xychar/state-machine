package com.xychar.stateful.exception;

public class StepNotFoundException extends RuntimeException {
    public StepNotFoundException(String message) {
        super(message);
    }

    public StepNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
