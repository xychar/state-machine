package com.xychar.stateful.engine;

public class RetryState {
    public String message;
    public Long nextWaiting;

    public Integer maxAttempts;
    public Integer firstInterval;
    public Integer intervalSeconds;
    public Double backoffRate;
    public Integer timeoutSeconds;

    public Class<? extends Throwable>[] exceptions;
    public Boolean succeedAfterRetrying;

    public void merge(RetryState that) {
        if (that.message != null) {
            this.message = that.message;
        }

        if (that.nextWaiting != null) {
            this.nextWaiting = that.nextWaiting;
        }

        if (that.maxAttempts != null) {
            this.maxAttempts = that.maxAttempts;
        }

        if (that.firstInterval != null) {
            this.firstInterval = that.firstInterval;
        }

        if (that.intervalSeconds != null) {
            this.intervalSeconds = that.intervalSeconds;
        }

        if (this.backoffRate != null) {
            this.backoffRate = that.backoffRate;
        }

        if (that.timeoutSeconds != null) {
            this.timeoutSeconds = that.timeoutSeconds;
        }

        if (that.exceptions != null) {
            this.exceptions = that.exceptions;
        }

        if (that.succeedAfterRetrying != null) {
            this.succeedAfterRetrying = that.succeedAfterRetrying;
        }
    }
}
