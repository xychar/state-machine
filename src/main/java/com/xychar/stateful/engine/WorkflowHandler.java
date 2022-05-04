package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.exception.SchedulingException;
import com.xychar.stateful.exception.StepFailedException;
import com.xychar.stateful.exception.StepStateException;
import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.Callable;

public class WorkflowHandler implements StepHandler, OutputHandler {
    private final WorkflowMetadata<?> metadata;
    private final WorkflowInstance<?> instance;
    private final StepStateAccessor accessor;

    private final WorkflowHandler parentHandler;
    private volatile WorkflowHandler asyncHandler;
    private volatile WorkflowHandler queryHandler;

    private static final ObjectMapper mapper = new ObjectMapper();

    public WorkflowHandler(WorkflowMetadata<?> metadata,
                           WorkflowInstance<?> instance,
                           StepStateAccessor accessor) {
        this.metadata = metadata;
        this.instance = instance;
        this.accessor = accessor;
        this.parentHandler = null;
        this.asyncHandler = null;
        this.queryHandler = null;
    }

    public WorkflowHandler(WorkflowHandler parent, WorkflowInstance<?> instance) {
        this.metadata = parent.metadata;
        this.accessor = parent.accessor;
        this.instance = instance;
        this.parentHandler = parent;
        this.asyncHandler = null;
        this.queryHandler = null;
    }

    public synchronized WorkflowInstance<?> async() {
        if (parentHandler != null) {
            return parentHandler.async();
        }

        if (asyncHandler == null) {
            WorkflowInstance<?> instance = metadata.newInstance();
            asyncHandler = new WorkflowHandler(this, instance);
            instance.handler = asyncHandler;
            return instance;
        }

        return asyncHandler.instance;
    }

    public synchronized WorkflowInstance<?> query() {
        if (parentHandler != null) {
            return parentHandler.query();
        }

        if (queryHandler == null) {
            WorkflowInstance<?> instance = metadata.newInstance();
            queryHandler = new WorkflowHandler(this, instance);
            instance.handler = queryHandler;
            return instance;
        }

        return queryHandler.instance;
    }

    public boolean isAsyncHandler() {
        return parentHandler != null && this == parentHandler.asyncHandler;
    }

    public boolean isQueryHandler() {
        return parentHandler != null && this == parentHandler.queryHandler;
    }

    private String encodeStepKey(Object[] stepKeys) throws Throwable {
        try {
            return mapper.writeValueAsString(stepKeys);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step key", e);
        }
    }

    @Override
    public Object intercept(WorkflowInstance<?> instance, int kind,
                            Method method, String stepKeyArgs,
                            Object... args) throws Throwable {
        System.out.println("*** invoking non-default method: " + method.getName());
        return null;
    }

    private void handleRetrying(StepState step, Method method) {

    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = encodeStepKey(StepKeyHelper.getStepKeys(stepKeyArgs, args));
        StepState step = accessor.load(instance.executionId, method, stepKey);
        if (step == null) {
            step = new StepState();
            step.startTime = Instant.now();
            step.currentRun = Instant.now();
        }

        step.stepMethod = method;
        if (isQueryHandler()) {
            StepStateHolder.setPreviousStepState(step);
            return null;
        }

        if (step.status != null) {
            System.out.format("found method-call: %s%s%n", method.getName(), stepKey);
            if (StepStatus.DONE.equals(step.status)) {
                return step.returnValue;
            } else if (StepStatus.FAILED.equals(step.status)) {
                throw new StepFailedException("Step already failed", step.exception);
            }

            step.currentRun = Instant.now();
        }

        StepStateHolder.setStepState(step);

        try {
            Object result = invocation.call();
            step.status = StepStatus.DONE;

            step.executionTimes++;
            step.endTime = Instant.now();

            step.exception = null;
            step.returnValue = result;
            step.parameters = args;

            if (step.result != null) {
                step.status = step.result;
            }

            accessor.save(instance.executionId, method, stepKey, step);
            return result;
        } catch (Exception e) {
            step.executionTimes++;
            step.exception = e;
            step.returnValue = null;
            step.parameters = args;

            Retry retryParams = method.getAnnotation(Retry.class);
            if (retryParams != null) {
                step.status = StepStatus.RETRYING;
                step.nextRun = Instant.now().plusSeconds(30L);
                accessor.save(instance.executionId, method, stepKey, step);

                SchedulingException scheduling = new SchedulingException();
                scheduling.stepState = step;
                scheduling.currentMethod = method;
                scheduling.waitingTime = retryParams.intervalSeconds() * 1000;
                throw scheduling;
            } else {
                step.status = StepStatus.FAILED;
                step.nextRun = Instant.now().plusSeconds(30L);
                accessor.save(instance.executionId, method, stepKey, step);
                throw e;
            }
        } finally {
            StepStateHolder.setStepState(null);
        }
    }

    @Override
    public Object intercept(WorkflowInstance<?> instance, int kind,
                            Method method, String stepKeyArgs,
                            Callable<?> invocation,
                            Object... args) throws Throwable {
        return invoke(instance, invocation, method, stepKeyArgs, args);
    }

    @Override
    public void sleep(long milliseconds) throws Throwable {
        System.out.println("*** sleep: " + milliseconds);
        Thread.sleep(milliseconds);
    }

    @Override
    public String getExecutionId() {
        return instance.executionId;
    }

    private StepState currentStepStateData() {
        StepState stateData = StepStateHolder.getStepState();
        if (stateData == null) {
            throw new WorkflowException("Step state is only available in step execution");
        } else {
            return stateData;
        }
    }

    @Override
    public Object property(OutputProxy parent, Method method,
                           Object... args) throws Throwable {
        System.out.println("*** handling output property: " + method.getName());
        return null;
    }
}
