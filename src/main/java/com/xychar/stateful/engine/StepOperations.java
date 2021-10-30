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
}
