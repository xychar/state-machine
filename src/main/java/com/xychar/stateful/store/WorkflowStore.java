package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.StepStateData;
import com.xychar.stateful.engine.StepStateException;
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

    public WorkflowRow loadState(String sessionId, String className, String methodName) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(WorkflowTable.TABLE.allColumns())
                .from(WorkflowTable.TABLE)
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(sessionId))
                .and(WorkflowTable.className, SqlBuilder.isEqualTo(className))
                .and(WorkflowTable.methodName, SqlBuilder.isEqualTo(methodName));

        return extensions.selectOne(selectStatement, WorkflowTable::mappingAllColumns).orElse(null);
    }

    public void saveState(WorkflowRow row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(WorkflowTable.TABLE)
                .set(WorkflowTable.state).equalToWhenPresent(row.state)
                .set(WorkflowTable.executions).equalToWhenPresent(row.executions)
                .set(WorkflowTable.exception).equalToWhenPresent(row.exception)
                .set(WorkflowTable.errorType).equalToWhenPresent(row.errorType)
                .set(WorkflowTable.startTime).equalToWhenPresent(row.startTime)
                .set(WorkflowTable.endTime).equalToWhenPresent(row.endTime)
                .set(WorkflowTable.returnValue).equalToWhenPresent(row.returnValue)
                .where(WorkflowTable.sessionId, SqlBuilder.isEqualTo(row.sessionId))
                .and(WorkflowTable.className, SqlBuilder.isEqualTo(row.className))
                .and(WorkflowTable.methodName, SqlBuilder.isEqualTo(row.methodName));

        int affectedRows = extensions.update(updateStatement);
        if (affectedRows == 0) {
            Buildable<GeneralInsertModel> insertStatement = GeneralInsertDSL.insertInto(WorkflowTable.TABLE)
                    .set(WorkflowTable.sessionId).toValue(row.sessionId)
                    .set(WorkflowTable.className).toValue(row.className)
                    .set(WorkflowTable.methodName).toValue(row.methodName)
                    .set(WorkflowTable.state).toValue(row.state)
                    .set(WorkflowTable.startTime).toValueWhenPresent(row.startTime)
                    .set(WorkflowTable.endTime).toValueWhenPresent(row.endTime)
                    .set(WorkflowTable.executions).toValueWhenPresent(row.executions)
                    .set(WorkflowTable.returnValue).toValueWhenPresent(row.returnValue)
                    .set(WorkflowTable.errorType).toValueWhenPresent(row.errorType)
                    .set(WorkflowTable.exception).toValueWhenPresent(row.exception);

            extensions.generalInsert(insertStatement);
        }
    }

    public StepStateData load(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        WorkflowRow row = loadState(sessionId, stepMethod.getName(), stepKey);
        if (row != null) {
            StepStateData stateData = new StepStateData();
            stateData.executionTimes = row.executions != null ? row.executions : 0;
            stateData.returnValue = null;
            stateData.parameters = new Object[0];
            stateData.exception = null;

            if (StringUtils.isNotBlank(row.startTime)) {
                stateData.startTime = Instant.parse(row.startTime);
            }

            if (StringUtils.isNotBlank(row.endTime)) {
                stateData.endTime = Instant.parse(row.endTime);
            }

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    stateData.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                try {
                    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                    stateData.exception = (Throwable) mapper.readValue(row.exception, errorType);
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step error", e);
                } catch (ClassNotFoundException e) {
                    throw new StepStateException("Error type not found", e);
                }
            }

            stateData.state = StepState.valueOf(row.state);
            return stateData;
        }

        return null;
    }

    public void save(String sessionId, String className, String methodName, StepStateData stateData) throws Throwable {
        WorkflowRow row = new WorkflowRow();
        row.sessionId = sessionId;

        try {
            row.returnValue = mapper.writeValueAsString(stateData.returnValue);
            System.out.println("ret: " + row.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step result", e);
        }

        try {
            if (stateData.exception != null) {
                row.errorType = stateData.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(stateData.exception);
                System.out.println("err: " + row.exception);
            }
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step error", e);
        }

        row.className = className;
        row.methodName = methodName;
        row.state = stateData.state.name();
        row.executions = stateData.executionTimes;
        row.startTime = stateData.startTime.toString();

        if (stateData.endTime != null) {
            row.endTime = stateData.endTime.toString();
        }

        saveState(row);
    }
}
