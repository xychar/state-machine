package com.xychar.stateful.engine;

public class WorkflowMetadata<T> {

    public Class<?> workflowClass;

    public Class<?> workflowProxyClass;

    public StepStateAccessor stateAccessor;

    public WorkflowSessionBase<T> newSession() {
        try {
            @SuppressWarnings("unchecked")
            WorkflowSessionBase<T> session = (WorkflowSessionBase<T>) workflowProxyClass.getConstructor().newInstance();
            session.handler = new WorkflowHandlerImpl(this, session, stateAccessor);
            return session;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow session", e);
        }
    }
}
