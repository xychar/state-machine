package com.xychar.stateful.engine;

public class StepStateHolder {
    private static final ThreadLocal<StepStateData> holder = new ThreadLocal<>();

    public static StepStateData getStepStateData() {
        return holder.get();
    }

    public static void setStepStateData(StepStateData stepStateData) {
        holder.set(stepStateData);
    }
}
