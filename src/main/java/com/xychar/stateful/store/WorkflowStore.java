package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.exception.DuplicatedStepException;
import com.xychar.stateful.exception.StepNotFoundException;
import com.xychar.stateful.exception.StepStateException;
import com.xychar.stateful.scheduler.WorkflowItem;
import com.xychar.stateful.engine.WorkflowState;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public WorkflowRow loadWorkflow(String sessionId, String className, String methodName, String stepKey) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(WorkflowTable.TABLE.allColumns())
                .from(WorkflowTable.TABLE)
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(sessionId))
                .and(WorkflowTable.className, SqlBuilder.isEqualTo(className))
                .and(WorkflowTable.methodName, SqlBuilder.isEqualTo(methodName))
                .and(WorkflowTable.stepKey, SqlBuilder.isEqualTo(stepKey));

        return extensions.selectOne(selectStatement, WorkflowTable::mappingAllColumns).orElse(null);
    }

    public List<WorkflowRow> fetchScheduledWorkflows(Long dueTime, long maxRowCount) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(WorkflowTable.TABLE.allColumns())
                .from(WorkflowTable.TABLE)
                .where(WorkflowTable.nextRun, SqlBuilder.isLessThan(dueTime))
                .and(WorkflowTable.state, SqlBuilder.isIn(
                        WorkflowState.Created.name(),
                        WorkflowState.Executing.name(),
                        WorkflowState.RollingBack.name()))
                .orderBy(WorkflowTable.nextRun)
                .limit(maxRowCount);

        return extensions.selectList(selectStatement, WorkflowTable::mappingAllColumns);
    }

    public boolean updateWorkflowNextRun(WorkflowRow row, Long currentTime, Long nextRunTime) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(WorkflowTable.TABLE)
                .set(WorkflowTable.nextRun).equalToWhenPresent(nextRunTime)
                .set(WorkflowTable.lastRun).equalToWhenPresent(currentTime)
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(row.sessionId))
                .and(WorkflowTable.className, SqlBuilder.isEqualTo(row.className))
                .and(WorkflowTable.methodName, SqlBuilder.isEqualTo(row.methodName))
                .and(WorkflowTable.stepKey, SqlBuilder.isEqualTo(row.stepKey))
                .and(WorkflowTable.nextRun, SqlBuilder.isEqualTo(row.nextRun));

        int affectedRows = extensions.update(updateStatement);
        return affectedRows > 0;
    }

    public void saveWorkflow(WorkflowRow row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(WorkflowTable.TABLE)
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
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(row.sessionId))
                .and(WorkflowTable.className, SqlBuilder.isEqualTo(row.className))
                .and(WorkflowTable.methodName, SqlBuilder.isEqualTo(row.methodName))
                .and(WorkflowTable.stepKey, SqlBuilder.isEqualTo(row.stepKey));

        int affectedRows = extensions.update(updateStatement);
        if (affectedRows == 0) {
            Buildable<GeneralInsertModel> insertStatement = GeneralInsertDSL.insertInto(WorkflowTable.TABLE)
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
                    .set(WorkflowTable.exception).toValueWhenPresent(row.exception);

            extensions.generalInsert(insertStatement);
        }
    }

    public Method getWorkflowMethod(String className, String methodName) throws Throwable {
        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> workflowClazz = Class.forName(className, true, threadClassLoader);

        return workflowClazz.getMethod(methodName);
    }

    public WorkflowItem transform(WorkflowRow row) throws Throwable {
        if (row != null) {
            WorkflowItem workflow = new WorkflowItem();
            workflow.executionTimes = row.executions != null ? row.executions : 0;
            workflow.returnValue = null;
            workflow.exception = null;

            Method stepMethod = getWorkflowMethod(row.className, row.methodName);

            if (StringUtils.isNotBlank(row.startTime)) {
                workflow.startTime = Instant.parse(row.startTime);
            }

            if (StringUtils.isNotBlank(row.endTime)) {
                workflow.endTime = Instant.parse(row.endTime);
            }

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    workflow.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            if (StringUtils.isNotBlank(row.parameters)) {
                try {
                    JsonNode jsonTree = mapper.readTree(row.parameters);
                    Class<?>[] paramTypes = stepMethod.getParameterTypes();
                    workflow.parameters = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        JsonNode param = jsonTree.get(i);
                        workflow.parameters[i] = mapper.treeToValue(param, paramTypes[i]);
                    }
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode workflow parameters", e);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                try {
                    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                    workflow.exception = (Throwable) mapper.readValue(row.exception, errorType);
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step error", e);
                } catch (ClassNotFoundException e) {
                    throw new StepStateException("Error type not found", e);
                }
            }

            workflow.lastRun = Instant.ofEpochMilli(row.lastRun);
            workflow.nextRun = Instant.ofEpochMilli(row.nextRun);
            workflow.state = WorkflowState.valueOf(row.state);
            return workflow;
        }

        return null;
    }

    public WorkflowItem load(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        String methodName = stepMethod.getName();
        String className = stepMethod.getDeclaringClass().getName();
        WorkflowRow row = loadWorkflow(sessionId, className, methodName, stepKey);
        return transform(row);
    }

    public WorkflowRow transform(WorkflowItem workflow) throws Throwable {
        if (workflow != null) {
            WorkflowRow row = new WorkflowRow();
            row.sessionId = workflow.sessionId;
            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.stepKey = workflow.stepKey;

            try {
                row.returnValue = mapper.writeValueAsString(workflow.returnValue);
                System.out.println("ret: " + row.returnValue);
            } catch (JsonProcessingException e) {
                throw new StepStateException("Failed to encode workflow result", e);
            }

            try {
                row.parameters = mapper.writeValueAsString(workflow.parameters);
                System.out.println("args: " + row.parameters);
            } catch (JsonProcessingException e) {
                throw new StepStateException("Failed to encode workflow parameters", e);
            }

            try {
                if (workflow.exception != null) {
                    row.errorType = workflow.exception.getClass().getName();
                    row.exception = errorMapper.writeValueAsString(workflow.exception);
                    System.out.println("err: " + row.exception);
                }
            } catch (JsonProcessingException e) {
                throw new StepStateException("Failed to encode workflow error", e);
            }

            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.state = workflow.state.name();
            row.executions = workflow.executionTimes;
            row.startTime = workflow.startTime.toString();

            if (workflow.endTime != null) {
                row.endTime = workflow.endTime.toString();
            }

            row.lastRun = workflow.lastRun.toEpochMilli();
            row.nextRun = workflow.nextRun.toEpochMilli();
            return row;
        }

        return null;
    }

    public void save(String sessionId, Method stepMethod, String stepKey, WorkflowItem workflow) throws Throwable {
        WorkflowRow row = new WorkflowRow();
        row.sessionId = sessionId;
        row.className = stepMethod.getDeclaringClass().getName();
        row.methodName = stepMethod.getName();
        row.stepKey = stepKey;

        try {
            row.returnValue = mapper.writeValueAsString(workflow.returnValue);
            System.out.println("ret: " + row.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode workflow result", e);
        }

        try {
            row.parameters = mapper.writeValueAsString(workflow.parameters);
            System.out.println("args: " + row.parameters);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode workflow parameters", e);
        }

        try {
            if (workflow.exception != null) {
                row.errorType = workflow.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(workflow.exception);
                System.out.println("err: " + row.exception);
            }
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode workflow error", e);
        }

        row.className = stepMethod.getDeclaringClass().getName();
        row.methodName = stepMethod.getName();
        row.state = workflow.state.name();
        row.executions = workflow.executionTimes;
        row.startTime = workflow.startTime.toString();
        row.lastRun = workflow.lastRun.toEpochMilli();
        row.nextRun = workflow.nextRun.toEpochMilli();

        if (workflow.endTime != null) {
            row.endTime = workflow.endTime.toString();
        }

        saveWorkflow(row);
    }
}
