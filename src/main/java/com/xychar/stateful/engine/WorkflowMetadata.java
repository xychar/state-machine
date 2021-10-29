package com.xychar.stateful.engine;

public class WorkflowMetadata<T> {

    public Class<?> workflowClass;

    public Class<?> workflowProxyClass;

    public StepStateAccessor stateAccessor;

    public WorkflowInstance<T> newSession() {
        try {
            @SuppressWarnings("unchecked")
            WorkflowInstance<T> inst = (WorkflowInstance<T>) workflowProxyClass.getConstructor().newInstance();
            inst.handler = new WorkflowHandler(this, inst, stateAccessor);
            return inst;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow session", e);
        }
    }
}
