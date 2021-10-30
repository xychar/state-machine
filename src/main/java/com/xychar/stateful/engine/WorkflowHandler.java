package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class WorkflowHandler implements StepHandler {
    private final WorkflowMetadata<?> metadata;
    private final WorkflowInstance<?> instance;
    private final StepStateAccessor accessor;

    private final WorkflowHandler parentHandler;
    private boolean isAsyncHandler = false;
    private boolean isQueryHandler = false;

    private static final ObjectMapper mapper = new ObjectMapper();

    public WorkflowHandler(WorkflowMetadata<?> metadata,
                           WorkflowInstance<?> instance,
                           StepStateAccessor accessor) {
        this.metadata = metadata;
        this.instance = instance;
        this.accessor = accessor;
        this.parentHandler = null;
    }

    public WorkflowHandler(WorkflowHandler parent,
                           boolean isAsyncHandler,
                           boolean isQueryHandler) {
        this.metadata = parent.metadata;
        this.instance = parent.instance;
        this.accessor = parent.accessor;
        this.parentHandler = parent;
        this.isAsyncHandler = isAsyncHandler;
        this.isQueryHandler = isQueryHandler;
    }

    public WorkflowHandler asyncHandler() {
        return new WorkflowHandler(this, true, false);
    }

    public WorkflowHandler queryHandler() {
        return new WorkflowHandler(this, false, true);
    }

    private int charToArgIndex(char value) {
        if (value >= '0' && value <= '9') {
            return (int) (value - '0');
        } else if (value >= 'a' && value <= 'z') {
            return (int) (value - 'a') + 10;
        } else {
            throw new IndexOutOfBoundsException("Invalid argument index");
        }
    }

    private Object[] getStepKeys(String stepKeyArgs, Object[] args) {
        if (stepKeyArgs != null && !stepKeyArgs.isEmpty()) {
            Object[] stepKeys = new Object[stepKeyArgs.length()];
            for (int i = 0; i < stepKeyArgs.length(); i++) {
                int argIndex = charToArgIndex(stepKeyArgs.charAt(i));
                stepKeys[i] = args[argIndex];
            }

            return stepKeys;
        } else {
            return new Object[0];
        }
    }

    private String encodeStepKey(Object[] stepKeys) throws Throwable {
        try {
            return mapper.writeValueAsString(stepKeys);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step key", e);
        }
    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = encodeStepKey(getStepKeys(stepKeyArgs, args));
        StepStateData stateData = accessor.load(instance.executionId, method, stepKey);
        if (stateData != null) {
            System.out.format("found method-call %s%s in cache%n", method.getName(), stepKey);
            if (StepState.Done.equals(stateData.state)) {
                return stateData.returnValue;
            } else if (StepState.Failed.equals(stateData.state)) {
                throw new StepFailedException("Step already failed", stateData.exception);
            }

            stateData.currentRun = Instant.now();
        } else {
            stateData = new StepStateData();
            stateData.startTime = Instant.now();
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
            stateData.state = StepState.Retrying;
            stateData.nextRun = Instant.now().plusSeconds(30L);
            accessor.save(instance.executionId, method, stepKey, stateData);

            SchedulingException scheduling = new SchedulingException();
            scheduling.stepStateData = stateData;
            scheduling.currentMethod = method;
            scheduling.waitingTime = 2500;
            throw scheduling;
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
    public void sleep(long milliseconds) {
        System.out.println("*** sleep: " + milliseconds);
    }

    @Override
    public String getExecutionId() {
        return instance.executionId;
    }

    @Override
    public void waitFor(long milliseconds) {
        System.out.println("*** waitFor: " + milliseconds);
    }

    private StepStateData currentStepStateData() {
        StepStateData stateData = StepStateHolder.getStepStateData();
        if (stateData != null) {
            return stateData;
        }

        throw new WorkflowException("Step state is only available in step execution");
    }

    @Override
    public int getExecutionTimes() {
        return currentStepStateData().executionTimes;
    }

    @Override
    public int getMaxAttempts() {
        return currentStepStateData().maxRetryTimes;
    }

    @Override
    public Instant getStepFirstRunTime() {
        return currentStepStateData().startTime;
    }

    @Override
    public Instant getStepThisRunTime() {
        return currentStepStateData().currentRun;
    }

    @Override
    public void succeed(String message) {
        StepStateData stateData = currentStepStateData();
        stateData.result = StepState.Done;
        stateData.message = message;
    }

    @Override
    public void retry(String message) {
        StepStateData stateData = currentStepStateData();
        stateData.result = StepState.Retrying;
        stateData.message = message;
    }

    @Override
    public void fail(String message) {
        StepStateData stateData = currentStepStateData();
        stateData.result = StepState.Failed;
        stateData.message = message;
    }

    @Override
    public StepState getStepStateOfLastCall() {
        return null;
    }

}
