package com.xychar.stateful.engine;

public class StepStateException extends Throwable {
    public StepStateException(String message) {
        super(message);
    }

    public StepStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

