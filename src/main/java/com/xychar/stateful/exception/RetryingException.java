package com.xychar.stateful.exception;

public class RetryingException extends RuntimeException {
    public String stepMessage;

    public int maxAttempts;
    public int firstInterval;
    public int intervalSeconds;
    public double backoffRate;
    public int timeoutSeconds;
    public boolean succeedAfterRetrying;

    public RetryingException(String message) {
        super(message);
    }

    public RetryingException(String message, Throwable cause) {
        super(message, cause);
    }
}

