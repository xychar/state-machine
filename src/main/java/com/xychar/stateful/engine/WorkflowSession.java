package com.xychar.stateful.engine;

public interface WorkflowSession<T> {
    String getSessionId();

    void setSessionId(String sessionId);

    T getWorkflowInstance();
}
