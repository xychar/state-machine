package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class WorkflowHandler implements StepHandler {

    private final WorkflowMetadata<?> metadata;
    private final WorkflowInstance<?> instance;
    private final StepStateAccessor accessor;

    private final ObjectMapper mapper = new ObjectMapper();

    static final Map<String, StepStateData> cache = new HashMap<>();

    public WorkflowHandler(WorkflowMetadata<?> metadata,
                           WorkflowInstance<?> instance,
                           StepStateAccessor accessor) {
        this.metadata = metadata;
        this.instance = instance;
        this.accessor = accessor;
    }

    private Object[] getStepKeys(String stepKeyArgs, Object[] args) {
        Object[] stepKeys = new Object[stepKeyArgs.length()];
        for (int i = 0; i < stepKeyArgs.length(); i++) {
            int argIndex = stepKeyArgs.charAt(i) - 'a';
            stepKeys[i] = args[argIndex];
        }

        return stepKeys;
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
            System.out.format("found method-call [%s:%s] in cache%n", method.getName(), stepKey);
            if (StepState.Done.equals(stateData.state)) {
                return stateData.returnValue;
            } else if (StepState.Failed.equals(stateData.state)) {
                throw new StepFailedException("Step already failed", stateData.exception);
            }
        }

        try {
            Object result = invocation.call();

            stateData = new StepStateData();
            stateData.exception = null;
            stateData.returnValue = result;
            stateData.parameters = args;

            stateData.state = StepState.Done;
            accessor.save(instance.executionId, method, stepKey, stateData);
            return result;
        } catch (Exception e) {
            stateData = new StepStateData();
            stateData.exception = e;
            stateData.returnValue = null;
            stateData.parameters = args;
            stateData.state = StepState.Retrying;
            stateData.nextRun = Instant.now().plusSeconds(30L);
            accessor.save(instance.executionId, method, stepKey, stateData);

            SchedulingException scheduling = new SchedulingException();
            scheduling.stepStateData = stateData;
            scheduling.currentMethod = method;
            throw scheduling;
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
    public void waitFor(long milliseconds) {
        System.out.println("*** waitFor: " + milliseconds);
    }

    @Override
    public String getExecutionId() {
        return instance.executionId;
    }
}
