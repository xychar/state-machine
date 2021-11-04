package com.xychar.stateful.store;

public class WorkflowRow {
    public String sessionId;
    public String className;
    public String methodName;
    public String stepKey;
    public String state;
    public String exception;
    public String errorType;
    public String startTime;
    public String endTime;
    public Long nextRun;
    public Long lastRun;
    public String returnValue;
    public String parameters;
    public Integer executions;
}
