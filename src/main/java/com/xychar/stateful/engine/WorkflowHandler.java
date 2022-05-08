package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
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
        retrying.maxAttempts = e.maxAttempts;
        retrying.message = e.stepMessage;
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
        if (e instanceof RetryingException) {
            retrying = getRetryStateFromException((RetryingException) e);
        } else if (step.retrying != null) {
            retrying = step.retrying;
        } else if (annotation != null) {
            retrying = getRetryStateFromAnnotation(annotation);
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
        if (step.executionTimes + 1 >= retrying.maxAttempts) {
            if (retrying.succeedAfterRetrying) {
                saveStep(accessor, step, StepStatus.DONE);
                return;
            } else {
                String message = "Exceeds max attempts, maxAttempts=" + retrying.maxAttempts;
                saveStep(accessor, step, StepStatus.FAILED);
                throw stepException != null ?
                        new StepFailedException(message, stepException) :
                        new StepFailedException(message);
            }
        }

        Duration totalExecutionTime = Duration.between(step.startTime, Instant.now());
        if (totalExecutionTime.getSeconds() > retrying.timeoutSeconds) {
            if (retrying.succeedAfterRetrying) {
                saveStep(accessor, step, StepStatus.DONE);
                return;
            } else {
                String message = "Step timeout, timeoutSeconds=" + retrying.timeoutSeconds;
                saveStep(accessor, step, StepStatus.FAILED);
                throw stepException != null ?
                        new TimeOutException(message, stepException) :
                        new TimeOutException(message);
            }
        }

        int waitingTimeSeconds = calculateRetryingInterval(step.executionTimes,
                retrying.firstInterval, retrying.intervalSeconds, retrying.backoffRate);

        SchedulingException scheduling = new SchedulingException();
        scheduling.waitingTime = 1000L * waitingTimeSeconds;
        scheduling.currentMethod = method;
        scheduling.stepState = step;

        step.nextRun = Instant.now().plusSeconds(waitingTimeSeconds);
        saveStep(accessor, step, StepStatus.RETRYING);
        throw scheduling;
    }

    private void handleRetryingException(StepState step, Method method, RetryState retrying) throws Throwable {
        Exception stepException = step.exception;

        // Exceeds max attempts
        if (step.executionTimes + 1 >= retrying.maxAttempts) {
            saveStep(accessor, step, StepStatus.FAILED);
            throw stepException;
        }

        // Retrying on specified exceptions
        if (retrying.exceptions != null && retrying.exceptions.length > 0) {
            // If the exception is not listed, then fails without retrying
            if (!Arrays.stream(retrying.exceptions).anyMatch(x -> x.isInstance(stepException))) {
                saveStep(accessor, step, StepStatus.FAILED);
                throw stepException;
            }
        }

        Duration totalExecutionTime = Duration.between(step.startTime, Instant.now());
        if (totalExecutionTime.getSeconds() > retrying.timeoutSeconds) {
            String message = "Step timeout, timeoutSeconds=" + retrying.timeoutSeconds;
            saveStep(accessor, step, StepStatus.FAILED);
            throw new TimeOutException(message, stepException);
        }

        int waitingTimeSeconds = calculateRetryingInterval(step.executionTimes,
                retrying.firstInterval, retrying.intervalSeconds, retrying.backoffRate);

        SchedulingException scheduling = new SchedulingException();
        scheduling.waitingTime = 1000L * waitingTimeSeconds;
        scheduling.currentMethod = method;
        scheduling.lastException = stepException;
        scheduling.stepState = step;

        step.nextRun = Instant.now().plusSeconds(waitingTimeSeconds);
        saveStep(accessor, step, StepStatus.RETRYING);
        throw scheduling;
    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());
        StepState savedStep = StepStateHolder.getStepState();

        String stepKey = encodeStepKey(StepKeyHelper.getStepKeys(stepKeyArgs, args));
        StepState step = accessor.load(instance.executionId, method, stepKey);
        if (step == null) {
            step = new StepState();
            step.startTime = Instant.now();
            step.currentRun = Instant.now();
        }

        step.handler = this;
        step.stepKey = stepKey;
        step.stepMethod = method;
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
            RetryState retrying = getRetryState(step, method, e);
            handleRetrying(step, method, StepStatus.RETRYING, retrying);
            return null;
        } catch (Exception e) {
            step.endTime = Instant.now();
            step.executionTimes++;
            step.exception = e;
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
