package com.xychar.stateful.engine;

import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Method;
import java.time.Instant;

/**
 * Utils to access step states in step executions.¬
 */
public class Steps {
    private static StepState currentStep() {
        StepState step = StepStateHolder.getStepState();
        if (step != null) {
            return step;
        }

        throw new WorkflowException("Step state is only available in step execution");
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

    public static void setExecutionTimes(int times) {
        currentStep().executionTimes = times;
    }

    public static Instant getStepFirstRunTime() {
        return currentStep().startTime;
    }

    public static Instant getStepThisRunTime() {
        return currentStep().currentRun;
    }

    public static void succeed(String message) {
        StepState step = currentStep();
        step.action = StepStatus.DONE;
        step.message = message;
    }

    public static void retry(String message) {
        StepState step = currentStep();
        step.action = StepStatus.RETRYING;
        step.message = message;
    }

    public static void retry(long nextWaiting, String message) {
        StepState step = currentStep();
        step.action = StepStatus.RETRYING;
        step.message = message;
        step.retrying = new RetryState();
        step.retrying.nextWaiting = nextWaiting;
    }

    public static void fail(String message) {
        StepState step = currentStep();
        step.action = StepStatus.FAILED;
        step.message = message;
    }

    public static void skip(String message) {
        StepState step = currentStep();
        step.action = StepStatus.SKIPPED;
        step.message = message;
    }

    public static Integer userVarInt() {
        return currentStep().userVarInt;
    }

    public static void userVarInt(Integer intValue) {
        StepState stateData = currentStep();
        stateData.userVarInt = intValue;
    }

    public static String userVarStr() {
        return currentStep().userVarStr;
    }

    public static void userVarStr(String strValue) {
        StepState step = currentStep();
        step.userVarStr = strValue;
    }

    public static <T> T userVarObj(Class<T> dataClass) {
        StepState step = currentStep();
        return step.handler.decodeObject(step.userVarObj, dataClass);
    }

    public static <T> void userVarObj(T userObj) {
        StepState step = currentStep();
        step.userVarObj = step.handler.encodeObject(userObj);
    }

    public static <T> T lastResult(Class<T> dataClass) {
        StepState step = currentStep();
        return step.handler.decodeObject(step.lastResult, dataClass);
    }

    public static <T> void lastResult(T userObj) {
        StepState step = currentStep();
        step.lastResult = step.handler.encodeObject(userObj);
    }

    @SuppressWarnings("unchecked")
    public static <T> T async(Class<T> workflowClass) {
        StepState step = currentStep();
        return ((WorkflowInstance<T>) step.handler.async()).getWorkflowInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> T query(Class<T> workflowClass) {
        StepState step = currentStep();
        return ((WorkflowInstance<T>) step.handler.query()).getWorkflowInstance();
    }

    public static String getExecutionId() {
        StepState step = currentStep();
        return step.executionId;
    }

    public static StepState getStepStateOfLastQuery() {
        StepState step = StepStateHolder.getQueryStepState();
        if (step != null) {
            return step;
        }

        throw new WorkflowException("Step state does not exist for query");
    }

    public static StepStatus getStepStatusOfLastQuery() {
        StepState step = StepStateHolder.getQueryStepState();
        return step != null ? step.status : StepStatus.CREATED;
    }
}
