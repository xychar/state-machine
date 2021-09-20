package com.xychar.stateful.engine;

public interface WorkflowOperations {

    String getSessionId();

    void waitFor(long milliseconds);

}
