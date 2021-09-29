package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface StepStateAccessor {
    StepStateData load(String sessionId, String stepName, String stepKey);

    void save(String sessionId, String stepName, String stepKey, StepStateData item);

    StepStateData load(String sessionId, Method stepMethod, String stepKey);

    void save(String sessionId, Method stepMethod, String stepKey, StepStateData item);
}
