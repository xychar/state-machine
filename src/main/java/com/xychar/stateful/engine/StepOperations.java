package com.xychar.stateful.engine;

import java.time.Instant;

/**
 * Step and workflow context.
 */
public interface StepOperations {

    String getExecutionId();

    void waitFor(long milliseconds);

    int getExecutionTimes();

    int getMaxRetryTimes();

    Instant getStepStartTime();

    Instant getStepRerunTime();

    /**
     * Mark step as successful.
     */
    void succeed(String message);

    /**
     * Mark step as retrying.
     */
    void retry(String message);

    /**
     * Mark step as failed.
     */
    void fail(String message);
}
