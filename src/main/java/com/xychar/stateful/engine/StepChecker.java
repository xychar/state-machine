package com.xychar.stateful.engine;

import com.xychar.stateful.exception.WorkflowException;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Utils to access step states in step executions.¬
 */
public class StepChecker {
    private static StepState currentStep() {
        StepState step = StepStateHolder.getStepState();
        if (step != null) {
            return step;
        }

        throw new WorkflowException("Step state is only available in step execution");
    }

    public static <T, R> StepStatus check1(Function<T, R> func) {
        StepState step = currentStep();
        @SuppressWarnings("unchecked")
        T instance = (T) step.handler.query();
        func.apply(instance);

        return getStepStatusOfLastQuery();
    }

    public static <T, A> StepStatus check1(BiConsumer<T, A> consumer, A param1) {
        StepState step = currentStep();
        @SuppressWarnings("unchecked")
        T instance = (T) step.handler.query();
        consumer.accept(instance, param1);

        return getStepStatusOfLastQuery();
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
