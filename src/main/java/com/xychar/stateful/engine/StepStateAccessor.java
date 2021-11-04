package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface StepStateAccessor {
    StepStateItem load(String sessionId, Method stepMethod, String stepKey) throws Throwable;

    void save(String sessionId, Method stepMethod, String stepKey, StepStateItem stateData) throws Throwable;
}
