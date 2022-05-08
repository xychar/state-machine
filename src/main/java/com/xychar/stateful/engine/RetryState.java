package com.xychar.stateful.engine;

public class RetryState {
    public String message;
    public int maxAttempts = 0;
    public int firstInterval = 0;
    public int intervalSeconds = 30;
    public double backoffRate = 1.0;
    public int timeoutSeconds = 300;
    public Class<? extends Throwable>[] exceptions;
    boolean succeedAfterRetrying = false;
}
