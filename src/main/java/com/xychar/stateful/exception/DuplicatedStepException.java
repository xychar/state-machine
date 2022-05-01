package com.xychar.stateful.exception;

public class DuplicatedStepException extends RuntimeException {
    public DuplicatedStepException(String message) {
        super(message);
    }

    public DuplicatedStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
