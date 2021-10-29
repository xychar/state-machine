package com.xychar.stateful.engine;

public interface StepOperations {

    String getExecutionId();

    void waitFor(long milliseconds);

}
