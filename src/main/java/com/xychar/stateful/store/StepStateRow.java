package com.xychar.stateful.store;

import com.xychar.stateful.engine.StepStatus;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepStateRow {
    public String executionId;
    public String stepName;
    public String stepKey;
    public String status;
    public String message;
    public String errorType;
    public String exception;
    public String lastResult;
    public String startTime;
    public String endTime;
    public String returnValue;
    public String parameters;
    public Integer executions;
    public Integer userVarInt;
    public String userVarStr;
    public String userVarObj;
}
