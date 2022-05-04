package com.xychar.stateful.engine;

import java.lang.reflect.Method;

public interface StepStateAccessor {
    StepState load(String executionId, Method stepMethod, String stepKey) throws Throwable;
    StepState loadMore(String executionId, Method stepMethod, String stepKey) throws Throwable;
    void save(String executionId, Method stepMethod, String stepKey, StepState data) throws Throwable;
}
