package com.xychar.stateful.mybatis;

public interface WorkflowMapper {
    int createTableIfNotExists();
    int createIndexIfNotExists();

    WorkflowRow load(String sessionId, String executionId, String workerName);

    int insert(WorkflowRow row);

    int update(WorkflowRow row);
}
