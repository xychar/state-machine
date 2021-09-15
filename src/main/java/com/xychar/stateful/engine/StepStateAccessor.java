package com.xychar.stateful.engine;

public interface StepStateAccessor {
    StepStateData load(String sessionId, String stepName, String stepKey);

    void save(String sessionId, String stepName, String stepKey, StepStateData item);
}
