package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface StepStateAccessor {
    StepState loadSimple(String executionId, Method stepMethod, String stepKey) throws Throwable;

    StepState loadFull(String executionId, Method stepMethod, String stepKey) throws Throwable;

    void save(String executionId, Method stepMethod, String stepKey, StepState data) throws Throwable;
}
