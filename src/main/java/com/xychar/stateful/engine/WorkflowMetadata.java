package com.xychar.stateful.engine;

import java.lang.reflect.InvocationTargetException;

public class WorkflowMetadata<T> {

    public Class<?> workflowClass;

    public Class<?> workflowProxyClass;

    public WorkflowSession<T> newSession() {
        try {
            @SuppressWarnings("unchecked")
            WorkflowSessionBase<T> session = (WorkflowSessionBase<T>) workflowProxyClass.getConstructor().newInstance();
            session.handler = new WorkflowHandler(this, session);
            return session;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow session", e);
        }
    }
}
