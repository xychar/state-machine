package com.xychar.stateful.exception;

public class UserDataException extends RuntimeException {
    public UserDataException(String message) {
        super(message);
    }

    public UserDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

