package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.engine.StepStatus;
import com.xychar.stateful.exception.StepStateException;
import com.xychar.stateful.mybatis.StepStateMapper;
import com.xychar.stateful.mybatis.StepStateRow;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepStateStore implements StepStateAccessor {
    private final StepStateMapper stepStateMapper;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final ObjectMapper errorMapper = new ObjectMapper()
            .addMixIn(Throwable.class, ThrowableMixIn.class);

    public StepStateStore(StepStateMapper stepStateMapper) {
        this.stepStateMapper = stepStateMapper;
    }

    public void createTableIfNotExists() {
        stepStateMapper.createTableIfNotExists();
    }

    private String getStepName(Method stepMethod) {
        String methodClassName = stepMethod.getDeclaringClass().getSimpleName();
        return methodClassName + "." + stepMethod.getName();
    }

    /**
     * Load fewer data for step scheduling.
     */
    @Override
    public StepState loadSimple(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        String stepName = getStepName(stepMethod);
        StepStateRow row = stepStateMapper.load(sessionId, stepName, stepKey);
        if (row != null) {
            StepState step = new StepState();
            step.executionTimes = row.executions != null ? row.executions : 0;
            step.returnValue = null;
            step.parameters = new Object[0];
            step.exception = null;

            step.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            step.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    step.returnValue = jsonMapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            step.status = StepStatus.valueOf(row.status);
            step.message = row.message;
            step.lastResult = row.lastResult;
            step.userVarInt = row.userVarInt;
            step.userVarStr = row.userVarStr;
            step.userVarObj = row.userVarObj;
            return step;
        }

        return null;
    }

    @Override
    public StepState loadFull(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        String stepName = getStepName(stepMethod);
        StepStateRow row = stepStateMapper.load(sessionId, stepName, stepKey);
        if (row != null) {
            StepState step = new StepState();
            step.executionTimes = row.executions != null ? row.executions : 0;
            step.returnValue = null;
            step.parameters = new Object[0];
            step.exception = null;

            step.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            step.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    step.returnValue = jsonMapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            if (StringUtils.isNotBlank(row.parameters)) {
                try {
                    JsonNode jsonTree = jsonMapper.readTree(row.parameters);
                    Class<?>[] paramTypes = stepMethod.getParameterTypes();
                    step.parameters = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        JsonNode param = jsonTree.get(i);
                        step.parameters[i] = jsonMapper.treeToValue(param, paramTypes[i]);
                    }
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step parameters", e);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                try {
                    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                    step.exception = (Exception) jsonMapper.readValue(row.exception, errorType);
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step error", e);
                } catch (ClassNotFoundException e) {
                    throw new StepStateException("Error type not found", e);
                }
            }

            step.status = StepStatus.valueOf(row.status);
            step.message = row.message;
            step.lastResult = row.lastResult;
            step.userVarInt = row.userVarInt;
            step.userVarStr = row.userVarStr;
            step.userVarObj = row.userVarObj;
            return step;
        }

        return null;
    }

    @Override
    public void save(String executionId, Method stepMethod, String stepKey, StepState step) throws Throwable {
        StepStateRow row = new StepStateRow();
        row.executionId = executionId;

        try {
            row.returnValue = jsonMapper.writeValueAsString(step.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step result", e);
        }

        try {
            row.parameters = jsonMapper.writeValueAsString(step.parameters);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step parameters", e);
        }

        try {
            if (step.exception != null) {
                row.errorType = step.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(step.exception);
            }
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step error", e);
        }

        row.stepKey = stepKey;
        row.stepName = getStepName(stepMethod);
        row.status = step.status.name();
        row.message = step.message;
        row.executions = step.executionTimes;
        row.lastResult = step.lastResult;
        row.userVarInt = step.userVarInt;
        row.userVarStr = step.userVarStr;
        row.userVarObj = step.userVarObj;

        row.startTime = Utils.callIfNotNull(step.startTime, Instant::toString);
        row.endTime = Utils.callIfNotNull(step.endTime, Instant::toString);

        if (stepStateMapper.update2(row) < 1) {
            stepStateMapper.insert(row);
        }
    }
}
