package com.xychar.stateful.engine;

import java.time.Instant;

/**
 * Step and workflow context.
 */
public interface StepOperations {

    String getExecutionId();

    void waitFor(long milliseconds);

    int getExecutionTimes();

    int getMaxAttempts();

    Instant getStepFirstRunTime();

    Instant getStepThisRunTime();

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

    /**
     * Can only be used following a step query.
     *
     * Example:
     *
     * query.myStep()
     * StepState myStepState = query.getStepStateOfLastCall();
     */
    StepState getStepStateOfLastCall();
}
