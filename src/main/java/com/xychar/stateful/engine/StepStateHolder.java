package com.xychar.stateful.engine;

public class StepStateHolder {
    private static final ThreadLocal<StepStateItem> holder = new ThreadLocal<>();
    private static final ThreadLocal<StepStateItem> previous = new ThreadLocal<>();

    public static StepStateItem getStepStateData() {
        return holder.get();
    }

    public static void setStepStateData(StepStateItem stepStateItem) {
        holder.set(stepStateItem);
    }

    public static StepStateItem getPreviousStepStateData() {
        return previous.get();
    }

    public static void setPreviousStepStateData(StepStateItem stepStateItem) {
        previous.set(stepStateItem);
    }
}
