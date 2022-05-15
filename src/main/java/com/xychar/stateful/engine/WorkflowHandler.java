package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.exception.RetryingException;
import com.xychar.stateful.exception.SchedulingException;
import com.xychar.stateful.exception.StepFailedException;
import com.xychar.stateful.exception.StepStateException;
import com.xychar.stateful.exception.TimeOutException;
import com.xychar.stateful.exception.UserDataException;
import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

public class WorkflowHandler implements StepHandler, OutputHandler {
    public final WorkflowInstance<?> instance;
    private final WorkflowMetadata<?> metadata;
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

    public StepState currentStepState() {
        StepState stateData = StepStateHolder.getStepState();
        if (stateData == null) {
            throw new WorkflowException("Step state is only available in step execution");
        } else {
            return stateData;
        }
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

    public <T> T decodeObject(String data, Class<T> dataClass) {
        try {
            return mapper.readValue(data, dataClass);
        } catch (JsonProcessingException e) {
            throw new UserDataException("Failed to decode user object", e);
        }
    }

    public <T> String encodeObject(T userObj) {
        try {
            return mapper.writeValueAsString(userObj);
        } catch (JsonProcessingException e) {
            throw new UserDataException("Failed to decode user object", e);
        }
    }

    private RetryState getRetryStateFromException(RetryingException e) {
        RetryState retrying = new RetryState();
        retrying.nextWaiting = e.nextWaiting;
        retrying.message = e.stepMessage;
        retrying.maxAttempts = e.maxAttempts;
        retrying.firstInterval = e.firstInterval;
        retrying.intervalSeconds = e.intervalSeconds;
        retrying.backoffRate = e.backoffRate;
        retrying.timeoutSeconds = e.timeoutSeconds;
        retrying.succeedAfterRetrying = e.succeedAfterRetrying;

        return retrying;
    }

    private RetryState getRetryStateFromAnnotation(Retry annotation) {
        RetryState retrying = new RetryState();
        retrying.maxAttempts = annotation.maxAttempts();
        retrying.firstInterval = annotation.firstInterval();
        retrying.intervalSeconds = annotation.intervalSeconds();
        retrying.backoffRate = annotation.backoffRate();
        retrying.timeoutSeconds = annotation.timeoutSeconds();
        retrying.exceptions = annotation.exceptions();
        retrying.succeedAfterRetrying = annotation.succeedAfterRetrying();

        return retrying;
    }

    private RetryState getRetryState(StepState step, Method method, Exception e) {
        // Priority of retrying strategy from different places:
        // retrying-exception > step-state > annotation
        RetryState retrying = new RetryState();
        Retry annotation = method.getAnnotation(Retry.class);
        if (annotation != null) {
            retrying.merge(getRetryStateFromAnnotation(annotation));
        }

        if (step.retrying != null) {
            retrying.merge(step.retrying);
        }

        if (e instanceof RetryingException) {
            retrying.merge(getRetryStateFromException((RetryingException) e));
        }

        return retrying;
    }

    private RetryState getRetryState(StepState step, Method method) {
        return getRetryState(step, method, null);
    }

    private int calculateRetryingInterval(
            int executionTimes, int firstInterval,
            int intervalSeconds, double backoffRate) {
        if (firstInterval > 0) {
            if (executionTimes == 0) {
                return firstInterval;
            }
        }

        double intervalFactor = 1.0;
        if (backoffRate > 1.00001) {
            intervalFactor = Math.pow(backoffRate, executionTimes);
        }

        return (int) (intervalSeconds * intervalFactor);
    }

    public void saveStep(StepStateAccessor accessor, StepState step, StepStatus action) throws Throwable {
        step.status = action;
        accessor.save(step.executionId, step.stepMethod, step.stepKey, step);
    }

    private void handleRetrying(StepState step, Method method, StepStatus action, RetryState retrying) throws Throwable {
        Exception stepException = step.exception;
        StepStatus status = StepStatus.DONE;
        if (action != null) {
            if (StepStatus.FAILED.equals(action)) {
                status = StepStatus.FAILED;
            } else if (StepStatus.DONE.equals(action)) {
                status = StepStatus.DONE;
            } else if (StepStatus.RETRYING.equals(action)) {
                status = StepStatus.RETRYING;
            }
        } else if (stepException != null) {
            status = StepStatus.RETRYING;
        }

        if (!StepStatus.RETRYING.equals(status)) {
            saveStep(accessor, step, status);
            return;
        }

        // Exceeds max attempts
        int maxAttempts = Utils.defaultIfNull(retrying.maxAttempts, 1);
        boolean succeedAfterRetrying = Utils.defaultIfNull(retrying.succeedAfterRetrying, false);
        if (step.executionTimes >= maxAttempts) {
            if (succeedAfterRetrying) {
                saveStep(accessor, step, StepStatus.DONE);
                return;
            } else {
                String message = "Exceeds max attempts, maxAttempts=" + maxAttempts;
                saveStep(accessor, step, StepStatus.FAILED);
                throw stepException != null ?
                        new StepFailedException(message, stepException) :
                        new StepFailedException(message);
            }
        }

        int timeoutSeconds = Utils.defaultIfNull(retrying.timeoutSeconds, 300);
        Duration totalExecutionTime = Duration.between(step.startTime, Instant.now());
        if (totalExecutionTime.getSeconds() > timeoutSeconds) {
            if (succeedAfterRetrying) {
                saveStep(accessor, step, StepStatus.DONE);
                return;
            } else {
                String message = "Step timeout, timeoutSeconds=" + timeoutSeconds;
                saveStep(accessor, step, StepStatus.FAILED);
                throw stepException != null ?
                        new TimeOutException(message, stepException) :
                        new TimeOutException(message);
            }
        }

        SchedulingException scheduling = new SchedulingException();

        if (retrying.nextWaiting != null) {
            scheduling.waitingTime = retrying.nextWaiting;
        } else {
            int firstInterval = Utils.defaultIfNull(retrying.firstInterval, 0);
            int intervalSeconds = Utils.defaultIfNull(retrying.intervalSeconds, 0);
            double backoffRate = Utils.defaultIfNull(retrying.backoffRate, 1.0);
            int waitingTimeSeconds = calculateRetryingInterval(
                    step.executionTimes, firstInterval, intervalSeconds, backoffRate);
            scheduling.waitingTime = 1000L * waitingTimeSeconds;
        }

        scheduling.currentMethod = method;
        scheduling.stepState = step;
        if (retrying.message != null) {
            step.message = retrying.message;
        }

        step.nextRun = Instant.now().plusMillis(scheduling.waitingTime);
        saveStep(accessor, step, StepStatus.RETRYING);
        throw scheduling;
    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());
        StepState savedStep = StepStateHolder.getStepState();

        String stepKey = encodeStepKey(StepKeyHelper.getStepKeys(stepKeyArgs, args));
        StepState step = accessor.loadSimple(instance.executionId, method, stepKey);
        if (step == null) {
            step = new StepState();
            step.startTime = Instant.now();
            step.currentRun = Instant.now();
        }

        step.handler = this;
        step.stepKey = stepKey;
        step.stepMethod = method;
        step.message = "";
        step.executionId = instance.executionId;
        if (isQueryHandler()) {
            StepStateHolder.setQueryStepState(step);
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
            step.endTime = Instant.now();

            step.executionTimes++;
            step.exception = null;
            step.returnValue = result;
            step.parameters = args;

            RetryState retrying = getRetryState(step, method);
            handleRetrying(step, method, step.action, retrying);
            return result;
        } catch (WorkflowException e) {
            throw e;
        } catch (RetryingException e) {
            step.message = e.getMessage();
            RetryState retrying = getRetryState(step, method, e);
            handleRetrying(step, method, StepStatus.RETRYING, retrying);
            return null;
        } catch (Exception e) {
            step.endTime = Instant.now();
            step.executionTimes++;
            step.exception = e;
            step.message = e.getMessage();
            step.returnValue = null;
            step.parameters = args;

            RetryState retrying = getRetryState(step, method);
            handleRetrying(step, method, null, retrying);
        } finally {
            // Restore stepState of parent call
            StepStateHolder.setStepState(savedStep);
        }

        return null;
    }

    @Override
    public Object interceptDefault(WorkflowInstance<?> instance, int kind,
                                   Method method, String stepKeyArgs,
                                   Method superMethod, Callable<?> invocation,
                                   Object... args) throws Throwable {
        return invoke(instance, invocation, method, stepKeyArgs, args);
    }

    @Override
    public Object interceptMethod(WorkflowInstance<?> instance, int kind,
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

    @Override
    public Object interceptProperty(OutputProxy parent, Method method,
                                    Object... args) throws Throwable {
        System.out.println("*** handling output property: " + method.getName());
        return null;
    }
}
