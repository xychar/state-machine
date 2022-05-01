package com.xychar.stateful.exception;

public class StepFailedException extends RuntimeException {
    public StepFailedException(String message) {
        super(message);
    }

    public StepFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
