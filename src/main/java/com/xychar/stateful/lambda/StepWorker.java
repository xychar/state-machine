package com.xychar.stateful.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowStatus;
import com.xychar.stateful.scheduler.WorkflowData;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowStore;

import java.lang.reflect.Method;
import java.time.Instant;

public class StepWorker extends StepInput {
    public WorkflowEngine workflowEngine;
    public StepStateStore stepStateStore;
    public WorkflowStore workflowStore;

    public Class<?> workflowClass = null;
    public JsonNode workflowConfig = null;
    public WorkflowData workflowItem = null;

    public void execute(StepInput event) throws Exception {
        Method stepMethod = Utils.getStepMethod(workflowClass, methodName);
        WorkflowMetadata<?> metadata = workflowEngine.buildFrom(workflowClass);

        WorkflowInstance<?> instance = workflowEngine.newInstance(metadata);
        instance.inputObject = workflowConfig;
        instance.workerName = workerName;

        workflowItem = workflowStore.load(sessionId, workerName);
        if (workflowItem == null) {
            workflowItem = workflowStore.createFrom(stepMethod);
            workflowItem.workerName = workerName;
            workflowItem.sessionId = sessionId;
            workflowItem.status = WorkflowStatus.EXECUTING;
            workflowItem.startTime = Instant.now();
            workflowStore.save(workflowItem);
        }

        instance.setExecutionId(workflowItem.executionId);
    }
}
