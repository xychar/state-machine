package com.xychar.stateful.mybatis;

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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getLastResult() {
        return lastResult;
    }

    public void setLastResult(String lastResult) {
        this.lastResult = lastResult;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Integer getExecutions() {
        return executions;
    }

    public void setExecutions(Integer executions) {
        this.executions = executions;
    }

    public Integer getUserVarInt() {
        return userVarInt;
    }

    public void setUserVarInt(Integer userVarInt) {
        this.userVarInt = userVarInt;
    }

    public String getUserVarStr() {
        return userVarStr;
    }

    public void setUserVarStr(String userVarStr) {
        this.userVarStr = userVarStr;
    }

    public String getUserVarObj() {
        return userVarObj;
    }

    public void setUserVarObj(String userVarObj) {
        this.userVarObj = userVarObj;
    }
}
