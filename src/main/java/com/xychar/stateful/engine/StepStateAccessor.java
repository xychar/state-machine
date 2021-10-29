package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface StepStateAccessor {
    StepStateData load(String sessionId, Method stepMethod, String stepKey) throws Throwable;

    void save(String sessionId, Method stepMethod, String stepKey, StepStateData stateData) throws Throwable;
}
