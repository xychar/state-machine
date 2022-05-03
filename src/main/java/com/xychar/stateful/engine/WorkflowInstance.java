package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.minidev.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowInstance<T> implements WorkflowExecution<T> {

    public String executionId;

    public StepHandler handler;

    public JsonNode inputObject = null;

    public final Map<Class<?>, Object> inputs = new LinkedHashMap<>();

    public final Map<Class<?>, OutputAccessor> outputs = new LinkedHashMap<>();

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public T getWorkflowInstance() {
        @SuppressWarnings("unchecked")
        T workflowInstance = (T) this;
        return workflowInstance;
    }
}
