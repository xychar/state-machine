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

    private void handleRetrying(StepStateItem stateData, Method method) {

    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = encodeStepKey(StepKeyHelper.getStepKeys(stepKeyArgs, args));
        StepStateItem stateData = accessor.load(instance.executionId, method, stepKey);
        if (stateData == null) {
            stateData = new StepStateItem();
            stateData.startTime = Instant.now();
            stateData.currentRun = Instant.now();
        }

        stateData.stepMethod = method;
        if (isQueryHandler()) {
            StepStateHolder.setPreviousStepStateData(stateData);
            return null;
        }

        if (stateData.state != null) {
            System.out.format("found method-call: %s%s%n", method.getName(), stepKey);
            if (StepState.Done.equals(stateData.state)) {
                return stateData.returnValue;
            } else if (StepState.Failed.equals(stateData.state)) {
                throw new StepFailedException("Step already failed", stateData.exception);
            }

            stateData.currentRun = Instant.now();
        }

        StepStateHolder.setStepStateData(stateData);

        try {
            Object result = invocation.call();
            stateData.state = StepState.Done;

            stateData.executionTimes++;
            stateData.endTime = Instant.now();

            stateData.exception = null;
            stateData.returnValue = result;
            stateData.parameters = args;

            if (stateData.result != null) {
                stateData.state = stateData.result;
            }

            accessor.save(instance.executionId, method, stepKey, stateData);
            return result;
        } catch (Exception e) {
            stateData.executionTimes++;
            stateData.exception = e;
            stateData.returnValue = null;
            stateData.parameters = args;

            Retry retryParams = method.getAnnotation(Retry.class);
            if (retryParams != null) {
                stateData.state = StepState.Retrying;
                stateData.nextRun = Instant.now().plusSeconds(30L);
                accessor.save(instance.executionId, method, stepKey, stateData);

                SchedulingException scheduling = new SchedulingException();
                scheduling.stepStateItem = stateData;
                scheduling.currentMethod = method;
                scheduling.waitingTime = retryParams.intervalSeconds() * 1000;
                throw scheduling;
            } else {
                stateData.state = StepState.Failed;
                stateData.nextRun = Instant.now().plusSeconds(30L);
                accessor.save(instance.executionId, method, stepKey, stateData);
                throw e;
            }
        } finally {
            StepStateHolder.setStepStateData(null);
        }
    }

    @Override
    public Object intercept(WorkflowInstance<?> instance, Callable<?> invocation,
                            Method method, String stepKeyArgs,
                            Object... args) throws Throwable {
        return invoke(instance, invocation, method, stepKeyArgs, args);
    }

    @Override
    public Object intercept(WorkflowInstance<?> instance,
                            Method method, String stepKeyArgs,
                            Object... args) throws Throwable {
        System.out.println("*** invoking non-default method: " + method.getName());
        return null;
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

    private StepStateItem currentStepStateData() {
        StepStateItem stateData = StepStateHolder.getStepStateData();
        if (stateData == null) {
            throw new WorkflowException("Step state is only available in step execution");
        } else {
            return stateData;
        }
    }

    @Override
    public Object property(OutputAccessor parent, Method method,
                           Object... args) throws Throwable {
        // Output access is not cached
        return null;
    }
}
