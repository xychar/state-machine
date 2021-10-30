package com.xychar.stateful.engine;

import java.lang.reflect.Method;
import java.time.Instant;

/**
 * Step and workflow context.
 */
public interface StepOperations {

    String getExecutionId();

    void waitFor(long milliseconds);

    Method getStepMethod();

    String getStepName();

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
     * <p>
     * Example:
     * <p>
     * query.myStep()
     * StepState myStepState = query.getStepStateOfLastCall();
     */
    StepState getStepStateOfLastCall();
}
