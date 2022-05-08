package com.xychar.stateful.engine;

import com.xychar.stateful.exception.WorkflowException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public static void fail(String message) {
        StepState step = currentStep();
        step.action = StepStatus.FAILED;
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

    public static <T, R> StepStatus check1(Function<T, R> func) {
        StepState step = currentStep();
        @SuppressWarnings("unchecked")
        T instance = (T) step.handler.query();
        func.apply(instance);

        StepState last = getStepStateOfLastQuery();
        return last.status;
    }

    public static <T, A> StepStatus check1(BiConsumer<T, A> consumer) {
        StepState step = currentStep();
        @SuppressWarnings("unchecked")
        T instance = (T) step.handler.query();
        consumer.accept(instance, null);

        StepState last = getStepStateOfLastQuery();
        return last.status;
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

    public static StepState getStepStateOfLastQuery() {
        StepState step = StepStateHolder.getQueryStepState();
        if (step != null) {
            return step;
        }

        throw new WorkflowException("Step state does not exist for query");
    }

    public static StepStatus getStepStatusOfLastQuery() {
        StepState step = StepStateHolder.getQueryStepState();
        if (step != null) {
            return step.status;
        } else {
            return StepStatus.CREATED;
        }
    }
}
