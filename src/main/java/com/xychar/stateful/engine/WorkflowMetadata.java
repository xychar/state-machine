package com.xychar.stateful.engine;

public class WorkflowMetadata<T> {

    public Class<?> workflowClass;

    public Class<?> workflowProxyClass;

    public WorkflowSession<T> newSession() {
        try {
            @SuppressWarnings("unchecked")
            WorkflowSessionBase<T> session = (WorkflowSessionBase<T>) workflowProxyClass.getConstructor().newInstance();
            session.handler = new StepHandler(this, session);
            session.delegate = new StepInterceptor(this, session);
            session.delegate2 = new StepInterceptor2(this, session);
            return session;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow session", e);
        }
    }
}
