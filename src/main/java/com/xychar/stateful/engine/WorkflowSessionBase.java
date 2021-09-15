package com.xychar.stateful.engine;

public class WorkflowSessionBase<T> implements WorkflowSession<T> {

    public String sessionId;

    public Interceptor handler;

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;

        return workflowInstance;
    }
}
