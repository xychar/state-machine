package com.xychar.stateful.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.WorkflowStatus;
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
import java.util.function.Function;

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
        jdbcTemplate.execute(WorkflowTable.CREATE_INDEX);
    }


    public WorkflowItem create(Method stepMethod) {
        WorkflowItem item = new WorkflowItem();

        item.executionId = UUID.randomUUID().toString();
        item.stepMethod = stepMethod;
        item.className = stepMethod.getDeclaringClass().getName();
        item.methodName = stepMethod.getName();
        item.status = WorkflowStatus.CREATED;
        return item;
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
                .set(WorkflowTable.status).equalToWhenPresent(row.status)
                .set(WorkflowTable.startTime).equalToWhenPresent(row.startTime)
                .set(WorkflowTable.endTime).equalToWhenPresent(row.endTime)
                .set(WorkflowTable.nextRun).equalToWhenPresent(row.nextRun)
                .set(WorkflowTable.lastRun).equalToWhenPresent(row.lastRun)
                .set(WorkflowTable.executions).equalToWhenPresent(row.executions)
                .set(WorkflowTable.returnValue).equalToWhenPresent(row.returnValue)
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
                    .set(WorkflowTable.status).toValue(row.status)
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

    public WorkflowItem load(String sessionId, String workerName) throws Exception {
        WorkflowRow row = loadWorkflow(sessionId, null, workerName);
        if (row != null) {
            WorkflowItem workflow = new WorkflowItem();
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

    public WorkflowItem loadMore(String sessionId, String workerName, Method stepMethod) throws Exception {
        WorkflowRow row = loadWorkflow(sessionId, null, workerName);
        if (row != null) {
            WorkflowItem workflow = new WorkflowItem();
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

    public void save(WorkflowItem workflow) throws Exception {
        if (workflow != null) {
            WorkflowRow row = new WorkflowRow();
            row.executionId = workflow.executionId;
            row.workerName = workflow.workerName;
            row.sessionId = workflow.sessionId;
            row.configData = workflow.configData;
            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();

            row.returnValue = mapper.writeValueAsString(workflow.returnValue);
            System.out.println("ret: " + row.returnValue);

            if (workflow.exception != null) {
                row.errorType = workflow.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(workflow.exception);
                System.out.println("err: " + row.exception);
            }

            row.className = workflow.stepMethod.getDeclaringClass().getName();
            row.methodName = workflow.stepMethod.getName();
            row.status = workflow.status.name();
            row.executions = workflow.executions;

            row.startTime = Utils.callIfNotNull(workflow.startTime, Instant::toString);
            row.endTime = Utils.callIfNotNull(workflow.endTime, Instant::toString);
            row.lastRun = Utils.callIfNotNull(workflow.lastRun, Instant::toEpochMilli);
            row.nextRun = Utils.callIfNotNull(workflow.nextRun, Instant::toEpochMilli);

            saveWorkflow(row);
        }
    }
}
