package com.xychar.stateful.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.WorkflowStatus;
import com.xychar.stateful.mybatis.WorkflowMapper;
import com.xychar.stateful.mybatis.WorkflowRow;
import com.xychar.stateful.scheduler.WorkflowData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

@Component
public class WorkflowStore {
    private final WorkflowMapper workflowMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    private final ObjectMapper errorMapper = new ObjectMapper()
            .addMixIn(Throwable.class, ThrowableMixIn.class);

    public WorkflowStore(@Autowired WorkflowMapper workflowMapper) {
        this.workflowMapper = workflowMapper;
    }

    public void createTableIfNotExists() {
        workflowMapper.createTableIfNotExists();
        workflowMapper.createIndexIfNotExists();
    }

    public WorkflowData createFrom(Method stepMethod) {
        WorkflowData item = new WorkflowData();

        item.executionId = UUID.randomUUID().toString();
        item.stepMethod = stepMethod;
        item.className = stepMethod.getDeclaringClass().getName();
        item.methodName = stepMethod.getName();
        item.status = WorkflowStatus.CREATED;
        return item;
    }

    public WorkflowData loadSimple(String sessionId, String workerName) throws Exception {
        WorkflowRow row = workflowMapper.load(sessionId, null, workerName);
        if (row != null) {
            WorkflowData workflow = new WorkflowData();
            workflow.executionId = row.executionId;
            workflow.workerName = row.workerName;
            workflow.sessionId = row.sessionId;
            workflow.executions = row.executions != null ? row.executions : 0;
            workflow.configData = row.configData;
            workflow.returnValue = null;
            workflow.exception = null;

            workflow.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            workflow.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            workflow.lastRun = Utils.callIfNotNull(row.lastRun, Instant::ofEpochMilli);
            workflow.nextRun = Utils.callIfNotNull(row.nextRun, Instant::ofEpochMilli);
            workflow.status = WorkflowStatus.valueOf(row.status);
            return workflow;
        }

        return null;
    }

    public WorkflowData loadFull(String sessionId, String workerName, Method stepMethod) throws Exception {
        WorkflowRow row = workflowMapper.load(sessionId, null, workerName);
        if (row != null) {
            WorkflowData workflow = new WorkflowData();
            workflow.executionId = row.executionId;
            workflow.workerName = row.workerName;
            workflow.sessionId = row.sessionId;
            workflow.executions = row.executions != null ? row.executions : 0;
            workflow.configData = row.configData;
            workflow.returnValue = null;
            workflow.exception = null;

            workflow.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            workflow.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            if (stepMethod != null && StringUtils.isNotBlank(row.returnValue)) {
                workflow.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                workflow.exception = (Throwable) mapper.readValue(row.exception, errorType);
            }

            workflow.lastRun = Utils.callIfNotNull(row.lastRun, Instant::ofEpochMilli);
            workflow.nextRun = Utils.callIfNotNull(row.nextRun, Instant::ofEpochMilli);
            workflow.status = WorkflowStatus.valueOf(row.status);
            return workflow;
        }

        return null;
    }

    public void save(WorkflowData workflow) throws Exception {
        if (workflow != null) {
            WorkflowRow row = new WorkflowRow();
            row.executionId = workflow.executionId;
            row.workerName = workflow.workerName;
            row.sessionId = workflow.sessionId;
            row.configData = workflow.configData;
            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();

            row.returnValue = mapper.writeValueAsString(workflow.returnValue);

            if (workflow.exception != null) {
                row.errorType = workflow.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(workflow.exception);
            }

            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.status = workflow.status.name();
            row.executions = workflow.executions;

            row.startTime = Utils.callIfNotNull(workflow.startTime, Instant::toString);
            row.endTime = Utils.callIfNotNull(workflow.endTime, Instant::toString);
            row.lastRun = Utils.callIfNotNull(workflow.lastRun, Instant::toEpochMilli);
            row.nextRun = Utils.callIfNotNull(workflow.nextRun, Instant::toEpochMilli);

            if (workflowMapper.update(row) < 1) {
                workflowMapper.insert(row);
            }
        }
    }
}
