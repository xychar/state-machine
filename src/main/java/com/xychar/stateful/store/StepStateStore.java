package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.StepStatus;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.exception.StepStateException;
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
import java.util.function.Function;

@Component
public class StepStateStore implements StepStateAccessor {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate template;

    private final ObjectMapper mapper = new ObjectMapper();

    private final ObjectMapper errorMapper = new ObjectMapper()
            .addMixIn(Throwable.class, ThrowableMixIn.class);

    public StepStateStore(@Autowired JdbcTemplate jdbcTemplate,
                          @Autowired NamedParameterJdbcTemplate template) {
        this.jdbcTemplate = jdbcTemplate;
        this.template = template;
    }

    public void createTableIfNotExists() {
        jdbcTemplate.execute(StepStateTable.CREATE_TABLE);
    }

    private String getStepName(Method stepMethod) {
        String methodClassName = stepMethod.getDeclaringClass().getSimpleName();
        return methodClassName + "." + stepMethod.getName();
    }

    public StepStateRow loadStepState(String sessionId, String stepName, String stepKey) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(StepStateTable.TABLE.allColumns())
                .from(StepStateTable.TABLE)
                .where(StepStateTable.executionId, SqlBuilder.isEqualTo(sessionId))
                .and(StepStateTable.stepName, SqlBuilder.isEqualTo(stepName))
                .and(StepStateTable.stepKey, SqlBuilder.isEqualTo(stepKey));

        return extensions.selectOne(selectStatement, StepStateTable::mappingAllColumns).orElse(null);
    }

    public void saveStepState(StepStateRow row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(StepStateTable.TABLE)
                .set(StepStateTable.status).equalToWhenPresent(row.status)
                .set(StepStateTable.message).equalToWhenPresent(row.message)
                .set(StepStateTable.executions).equalToWhenPresent(row.executions)
                .set(StepStateTable.exception).equalToWhenPresent(row.exception)
                .set(StepStateTable.errorType).equalToWhenPresent(row.errorType)
                .set(StepStateTable.lastResult).equalToWhenPresent(row.lastResult)
                .set(StepStateTable.startTime).equalToWhenPresent(row.startTime)
                .set(StepStateTable.endTime).equalToWhenPresent(row.endTime)
                .set(StepStateTable.parameters).equalToWhenPresent(row.parameters)
                .set(StepStateTable.returnValue).equalToWhenPresent(row.returnValue)
                .set(StepStateTable.userVarInt).equalToWhenPresent(row.userVarInt)
                .set(StepStateTable.userVarStr).equalToWhenPresent(row.userVarStr)
                .set(StepStateTable.userVarObj).equalToWhenPresent(row.userVarObj)
                .where(StepStateTable.executionId, SqlBuilder.isEqualTo(row.executionId))
                .and(StepStateTable.stepName, SqlBuilder.isEqualTo(row.stepName))
                .and(StepStateTable.stepKey, SqlBuilder.isEqualTo(row.stepKey));

        int affectedRows = extensions.update(updateStatement);
        if (affectedRows == 0) {
            Buildable<GeneralInsertModel> insertStatement = GeneralInsertDSL.insertInto(StepStateTable.TABLE)
                    .set(StepStateTable.executionId).toValue(row.executionId)
                    .set(StepStateTable.stepName).toValue(row.stepName)
                    .set(StepStateTable.stepKey).toValue(row.stepKey)
                    .set(StepStateTable.status).toValue(row.status)
                    .set(StepStateTable.message).toValue(row.message)
                    .set(StepStateTable.startTime).toValueWhenPresent(row.startTime)
                    .set(StepStateTable.endTime).toValueWhenPresent(row.endTime)
                    .set(StepStateTable.executions).toValueWhenPresent(row.executions)
                    .set(StepStateTable.returnValue).toValueWhenPresent(row.returnValue)
                    .set(StepStateTable.parameters).toValueWhenPresent(row.parameters)
                    .set(StepStateTable.errorType).toValueWhenPresent(row.errorType)
                    .set(StepStateTable.exception).toValueWhenPresent(row.exception)
                    .set(StepStateTable.lastResult).toValueWhenPresent(row.lastResult)
                    .set(StepStateTable.userVarInt).toValueWhenPresent(row.userVarInt)
                    .set(StepStateTable.userVarStr).toValueWhenPresent(row.userVarStr)
                    .set(StepStateTable.userVarObj).toValueWhenPresent(row.userVarObj);

            extensions.generalInsert(insertStatement);
        }
    }

    /**
     * Load fewer data for step scheduling.
     */
    @Override
    public StepState load(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        String stepName = getStepName(stepMethod);
        StepStateRow row = loadStepState(sessionId, stepName, stepKey);
        if (row != null) {
            StepState step = new StepState();
            step.executionTimes = row.executions != null ? row.executions : 0;
            step.returnValue = null;
            step.parameters = new Object[0];
            step.exception = null;

            step.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            step.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    step.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            step.status = StepStatus.valueOf(row.status);
            step.message = row.message;
            step.lastResult = row.lastResult;
            step.userVarInt = row.userVarInt;
            step.userVarStr = row.userVarStr;
            step.userVarObj = row.userVarObj;
            return step;
        }

        return null;
    }

    @Override
    public StepState loadMore(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        String stepName = getStepName(stepMethod);
        StepStateRow row = loadStepState(sessionId, stepName, stepKey);
        if (row != null) {
            StepState step = new StepState();
            step.executionTimes = row.executions != null ? row.executions : 0;
            step.returnValue = null;
            step.parameters = new Object[0];
            step.exception = null;

            step.startTime = Utils.callIfNotBlank(row.startTime, Instant::parse);
            step.endTime = Utils.callIfNotBlank(row.endTime, Instant::parse);

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    step.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            if (StringUtils.isNotBlank(row.parameters)) {
                try {
                    JsonNode jsonTree = mapper.readTree(row.parameters);
                    Class<?>[] paramTypes = stepMethod.getParameterTypes();
                    step.parameters = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        JsonNode param = jsonTree.get(i);
                        step.parameters[i] = mapper.treeToValue(param, paramTypes[i]);
                    }
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step parameters", e);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                try {
                    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                    step.exception = (Throwable) mapper.readValue(row.exception, errorType);
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step error", e);
                } catch (ClassNotFoundException e) {
                    throw new StepStateException("Error type not found", e);
                }
            }

            step.status = StepStatus.valueOf(row.status);
            step.message = row.message;
            step.lastResult = row.lastResult;
            step.userVarInt = row.userVarInt;
            step.userVarStr = row.userVarStr;
            step.userVarObj = row.userVarObj;
            return step;
        }

        return null;
    }

    @Override
    public void save(String executionId, Method stepMethod, String stepKey, StepState step) throws Throwable {
        StepStateRow row = new StepStateRow();
        row.executionId = executionId;

        try {
            row.returnValue = mapper.writeValueAsString(step.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step result", e);
        }

        try {
            row.lastResult = mapper.writeValueAsString(step.lastResult);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode last result", e);
        }

        try {
            row.parameters = mapper.writeValueAsString(step.parameters);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step parameters", e);
        }

        try {
            if (step.exception != null) {
                row.errorType = step.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(step.exception);
            }
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step error", e);
        }

        row.stepKey = stepKey;
        row.stepName = getStepName(stepMethod);
        row.status = step.status.name();
        row.message = step.message;
        row.executions = step.executionTimes;
        row.userVarInt = step.userVarInt;
        row.userVarStr = step.userVarStr;
        row.userVarObj = step.userVarObj;

        row.startTime = Utils.callIfNotNull(step.startTime, Instant::toString);
        row.endTime = Utils.callIfNotNull(step.endTime, Instant::toString);

        saveStepState(row);
    }
}
