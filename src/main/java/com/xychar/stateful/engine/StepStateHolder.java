package com.xychar.stateful.engine;

/**
 * Thread local globals for step state access during step execution.
 */
public class StepStateHolder {
    private static final ThreadLocal<StepState> holder = new ThreadLocal<>();
    private static final ThreadLocal<StepState> query = new ThreadLocal<>();

    public static StepState getStepState() {
        return holder.get();
    }

    public static void setStepState(StepState step) {
        holder.set(step);
    }

    public static StepState getQueryStepState() {
        return query.get();
    }

    public static void setQueryStepState(StepState step) {
        query.set(step);
    }
}
