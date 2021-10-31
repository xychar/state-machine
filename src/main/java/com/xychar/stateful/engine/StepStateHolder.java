package com.xychar.stateful.engine;

public class StepStateHolder {
    private static final ThreadLocal<StepStateData> holder = new ThreadLocal<>();
    private static final ThreadLocal<StepStateData> previous = new ThreadLocal<>();

    public static StepStateData getStepStateData() {
        return holder.get();
    }

    public static void setStepStateData(StepStateData stepStateData) {
        holder.set(stepStateData);
    }

    public static StepStateData getPreviousStepStateData() {
        return previous.get();
    }

    public static void setPreviousStepStateData(StepStateData stepStateData) {
        previous.set(stepStateData);
    }
}
