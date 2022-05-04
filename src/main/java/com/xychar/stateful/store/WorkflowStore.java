package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.engine.WorkflowState;
import com.xychar.stateful.exception.WorkflowException;
import com.xychar.stateful.scheduler.WorkflowItem;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.GeneralInsertDSL;
import org.mybatis.dynamic.sql.insert.GeneralInsertModel;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.Buildable;
import org.mybatis.dynamic.sql.util.spring.NamedParameterJdbcTemplateExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

@Component
public class WorkflowStore {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate template;

    private final ObjectMapper mapper = new ObjectMapper();

    private final ObjectMapper errorMapper = new ObjectMapper()
            .addMixIn(Throwable.class, ThrowableMixIn.class);

    public WorkflowStore(@Autowired JdbcTemplate jdbcTemplate,
                         @Autowired NamedParameterJdbcTemplate template) {
        this.jdbcTemplate = jdbcTemplate;
        this.template = template;
    }

    public void createTableIfNotExists() {
        jdbcTemplate.execute(WorkflowTable.CREATE_TABLE);
    }

    public WorkflowItem create(Method stepMethod, String params) {
        WorkflowItem item = new WorkflowItem();

        item.executionId = UUID.randomUUID().toString();
        item.stepMethod = stepMethod;
        item.className = stepMethod.getDeclaringClass().getName();
        item.methodName = stepMethod.getName();
        item.state = WorkflowState.Created;

        try {
            JsonNode jsonTree = mapper.createArrayNode();
            if (StringUtils.isNotBlank(params)) {
                jsonTree = mapper.readTree(params);
            }

            Class<?>[] paramTypes = item.stepMethod.getParameterTypes();
            Object[] parameters = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                JsonNode param = jsonTree.get(i);
                parameters[i] = mapper.treeToValue(param, paramTypes[i]);
            }

            item.parameters = parameters;
            return item;
        } catch (JsonProcessingException e) {
            throw new WorkflowException("Failed to build workflow step", e);
        }
    }

    public WorkflowRow loadWorkflow(String sessionId, String executionId, String workerName) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(WorkflowTable.TABLE.allColumns())
                .from(WorkflowTable.TABLE)
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(sessionId))
                .and(WorkflowTable.executionId, SqlBuilder.isEqualToWhenPresent(executionId))
                .and(WorkflowTable.workerName, SqlBuilder.isEqualToWhenPresent(workerName));

        return extensions.selectOne(selectStatement, WorkflowTable::mappingAllColumns).orElse(null);
    }

    public void saveWorkflow(WorkflowRow row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(WorkflowTable.TABLE)
                .set(WorkflowTable.workerName).equalToWhenPresent(row.workerName)
                .set(WorkflowTable.sessionId).equalToWhenPresent(row.sessionId)
                .set(WorkflowTable.state).equalToWhenPresent(row.state)
                .set(WorkflowTable.startTime).equalToWhenPresent(row.startTime)
                .set(WorkflowTable.endTime).equalToWhenPresent(row.endTime)
                .set(WorkflowTable.nextRun).equalToWhenPresent(row.nextRun)
                .set(WorkflowTable.lastRun).equalToWhenPresent(row.lastRun)
                .set(WorkflowTable.executions).equalToWhenPresent(row.executions)
                .set(WorkflowTable.returnValue).equalToWhenPresent(row.returnValue)
                .set(WorkflowTable.parameters).equalToWhenPresent(row.parameters)
                .set(WorkflowTable.errorType).equalToWhenPresent(row.errorType)
                .set(WorkflowTable.exception).equalToWhenPresent(row.exception)
                .set(WorkflowTable.configData).equalToWhenPresent(row.configData)
                .where(WorkflowTable.executionId, SqlBuilder.isEqualTo(row.executionId));

        int affectedRows = extensions.update(updateStatement);
        if (affectedRows == 0) {
            Buildable<GeneralInsertModel> insertStatement = GeneralInsertDSL.insertInto(WorkflowTable.TABLE)
                    .set(WorkflowTable.executionId).toValue(row.executionId)
                    .set(WorkflowTable.workerName).toValue(row.workerName)
                    .set(WorkflowTable.sessionId).toValue(row.sessionId)
                    .set(WorkflowTable.className).toValue(row.className)
                    .set(WorkflowTable.methodName).toValue(row.methodName)
                    .set(WorkflowTable.stepKey).toValue(row.stepKey)
                    .set(WorkflowTable.state).toValue(row.state)
                    .set(WorkflowTable.startTime).toValueWhenPresent(row.startTime)
                    .set(WorkflowTable.endTime).toValueWhenPresent(row.endTime)
                    .set(WorkflowTable.nextRun).toValueWhenPresent(row.nextRun)
                    .set(WorkflowTable.lastRun).toValueWhenPresent(row.lastRun)
                    .set(WorkflowTable.executions).toValueWhenPresent(row.executions)
                    .set(WorkflowTable.returnValue).toValueWhenPresent(row.returnValue)
                    .set(WorkflowTable.errorType).toValueWhenPresent(row.errorType)
                    .set(WorkflowTable.exception).toValueWhenPresent(row.exception)
                    .set(WorkflowTable.configData).toValueWhenPresent(row.configData);

            extensions.generalInsert(insertStatement);
        }
    }

    public WorkflowItem transform(WorkflowRow row, Method stepMethod) throws Exception {
        if (row != null) {
            WorkflowItem workflow = new WorkflowItem();
            workflow.executionId = row.executionId;
            workflow.workerName = row.workerName;
            workflow.sessionId = row.sessionId;
            workflow.executionTimes = row.executions != null ? row.executions : 0;
            workflow.configData = row.configData;
            workflow.returnValue = null;
            workflow.exception = null;

            if (StringUtils.isNotBlank(row.startTime)) {
                workflow.startTime = Instant.parse(row.startTime);
            }

            if (StringUtils.isNotBlank(row.endTime)) {
                workflow.endTime = Instant.parse(row.endTime);
            }

            if (stepMethod != null && StringUtils.isNotBlank(row.returnValue)) {
                workflow.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
            }

            if (stepMethod != null && StringUtils.isNotBlank(row.parameters)) {
                JsonNode jsonTree = mapper.readTree(row.parameters);
                Class<?>[] paramTypes = stepMethod.getParameterTypes();
                workflow.parameters = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    JsonNode param = jsonTree.get(i);
                    workflow.parameters[i] = mapper.treeToValue(param, paramTypes[i]);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                workflow.exception = (Throwable) mapper.readValue(row.exception, errorType);
            }

            workflow.lastRun = Instant.ofEpochMilli(row.lastRun);
            workflow.nextRun = Instant.ofEpochMilli(row.nextRun);
            workflow.state = WorkflowState.valueOf(row.state);
            return workflow;
        }

        return null;
    }

    public WorkflowItem load(String sessionId, String workerName, Method stepMethod) throws Exception {
        WorkflowRow row = loadWorkflow(sessionId, null, workerName);
        return transform(row, stepMethod);
    }

    public WorkflowRow transform(WorkflowItem workflow) throws Exception {
        if (workflow != null) {
            WorkflowRow row = new WorkflowRow();
            row.executionId = workflow.executionId;
            row.workerName = workflow.workerName;
            row.sessionId = workflow.sessionId;
            row.configData = workflow.configData;
            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.stepKey = workflow.stepKey;

            row.returnValue = mapper.writeValueAsString(workflow.returnValue);
            System.out.println("ret: " + row.returnValue);

            row.parameters = mapper.writeValueAsString(workflow.parameters);
            System.out.println("args: " + row.parameters);

            if (workflow.exception != null) {
                row.errorType = workflow.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(workflow.exception);
                System.out.println("err: " + row.exception);
            }

            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.state = workflow.state.name();
            row.executions = workflow.executionTimes;

            if (workflow.startTime != null) {
                row.startTime = workflow.startTime.toString();
            }

            if (workflow.endTime != null) {
                row.endTime = workflow.endTime.toString();
            }

            if (workflow.lastRun != null) {
                row.lastRun = workflow.lastRun.toEpochMilli();
            }

            if (workflow.nextRun != null) {
                row.nextRun = workflow.nextRun.toEpochMilli();
            }

            return row;
        }

        return null;
    }

    public void save(WorkflowItem workflow) throws Exception {
        saveWorkflow(transform(workflow));
    }
}
