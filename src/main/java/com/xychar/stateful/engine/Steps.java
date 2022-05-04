package com.xychar.stateful.engine;

import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Method;
import java.time.Instant;

/**
 * Utils to access step states in step executions.¬
 */
public class Steps {
    private static StepState currentStep() {
        StepState stateData = StepStateHolder.getStepState();
        if (stateData == null) {
            throw new WorkflowException("Step state is only available in step execution");
        } else {
            return stateData;
        }
    }

    public static Method getStepMethod() {
        return currentStep().stepMethod;
    }

    public static String getStepName() {
        return currentStep().stepMethod.getName();
    }

    public static int getExecutionTimes() {
        return currentStep().executionTimes;
    }

    public static int getMaxAttempts() {
        return currentStep().maxAttempts;
    }

    public static Instant getStepFirstRunTime() {
        return currentStep().startTime;
    }

    public static Instant getStepThisRunTime() {
        return currentStep().currentRun;
    }

    public static void succeed(String message) {
        StepState stateData = currentStep();
        stateData.result = StepStatus.DONE;
        stateData.message = message;
    }

    public static void retry(String message) {
        StepState stateData = currentStep();
        stateData.result = StepStatus.RETRYING;
        stateData.message = message;
    }

    public static void fail(String message) {
        StepState stateData = currentStep();
        stateData.result = StepStatus.FAILED;
        stateData.message = message;
    }

    public static String userVarStr() {
        return currentStep().userVarStr;
    }

    public static void userVarStr(String strValue) {
        StepState stateData = currentStep();
        stateData.userVarStr = strValue;
    }

    public static Integer userVarInt() {
        return currentStep().userVarInt;
    }

    public static void userVarInt(Integer intValue) {
        StepState stateData = currentStep();
        stateData.userVarInt = intValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T async(T that) {
        WorkflowInstance<T> instance = (WorkflowInstance<T>) that;
        WorkflowHandler handler = (WorkflowHandler) instance.handler;
        return ((WorkflowInstance<T>) handler.async()).getWorkflowInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> T query(T that) {
        WorkflowInstance<T> instance = (WorkflowInstance<T>) that;
        WorkflowHandler handler = (WorkflowHandler) instance.handler;
        return ((WorkflowInstance<T>) handler.query()).getWorkflowInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> String getExecutionId(T that) {
        WorkflowInstance<T> instance = (WorkflowInstance<T>) that;
        return instance.executionId;
    }

    @SuppressWarnings("unchecked")
    public static <T> void sleep(T that, long milliseconds) {
        WorkflowInstance<T> instance = (WorkflowInstance<T>) that;
        System.out.println("*** executionId: " + instance.executionId +
                " sleep: " + milliseconds);
    }

    public static StepStatus getStepStateOfLastCall() {
        StepState stateData = StepStateHolder.getPreviousStepState();
        if (stateData != null) {
            return stateData.status;
        } else {
            return StepStatus.CREATED;
        }
    }
}
