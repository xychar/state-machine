package com.xychar.stateful.engine;

/**
 * Thread local globals for step state access during step execution.
 */
public class StepStateHolder {
    private static final ThreadLocal<StepState> holder = new ThreadLocal<>();
    private static final ThreadLocal<StepState> previous = new ThreadLocal<>();

    public static StepState getStepState() {
        return holder.get();
    }

    public static void setStepState(StepState step) {
        holder.set(step);
    }

    public static StepState getPreviousStepState() {
        return previous.get();
    }

    public static void setPreviousStepState(StepState step) {
        previous.set(step);
    }
}
