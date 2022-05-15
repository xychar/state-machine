package com.xychar.stateful.mybatis;

public class WorkflowRow {
    public String executionId;
    public String workerName;
    public String sessionId;
    public String className;
    public String methodName;
    public String status;
    public Integer executions;
    public String startTime;
    public String endTime;
    public Long nextRun;
    public Long lastRun;
    public String returnValue;
    public String exception;
    public String errorType;
    public String configData;
}
