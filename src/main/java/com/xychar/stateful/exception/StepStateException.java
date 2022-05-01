package com.xychar.stateful.exception;

public class StepStateException extends Throwable {
    public StepStateException(String message) {
        super(message);
    }

    public StepStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

