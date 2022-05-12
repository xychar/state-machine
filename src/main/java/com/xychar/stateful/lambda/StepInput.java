package com.xychar.stateful.lambda;

import com.xychar.stateful.engine.WorkflowStatus;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepInput {
    public String executionId;
    public String workerName;
    public String sessionId;
    public String className;
    public String methodName;
    public String configData;
}
