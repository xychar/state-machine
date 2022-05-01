package com.xychar.stateful.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.engine.StepStateItem;
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

    public StepStateRow loadState(String sessionId, String stepName, String stepKey) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(StepStateTable.TABLE.allColumns())
                .from(StepStateTable.TABLE)
                .where(StepStateTable.sessionId, SqlBuilder.isEqualTo(sessionId))
                .and(StepStateTable.stepName, SqlBuilder.isEqualTo(stepName))
                .and(StepStateTable.stepKey, SqlBuilder.isEqualTo(stepKey));

        return extensions.selectOne(selectStatement, StepStateTable::mappingAllColumns).orElse(null);
    }

    public void saveState(StepStateRow row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL<UpdateModel>.UpdateWhereBuilder updateStatement = UpdateDSL.update(StepStateTable.TABLE)
                .set(StepStateTable.state).equalToWhenPresent(row.state)
                .set(StepStateTable.executions).equalToWhenPresent(row.executions)
                .set(StepStateTable.exception).equalToWhenPresent(row.exception)
                .set(StepStateTable.errorType).equalToWhenPresent(row.errorType)
                .set(StepStateTable.startTime).equalToWhenPresent(row.startTime)
                .set(StepStateTable.endTime).equalToWhenPresent(row.endTime)
                .set(StepStateTable.parameters).equalToWhenPresent(row.parameters)
                .set(StepStateTable.returnValue).equalToWhenPresent(row.returnValue)
                .set(StepStateTable.userVarStr).equalToWhenPresent(row.userVarStr)
                .set(StepStateTable.userVarInt).equalToWhenPresent(row.userVarInt)
                .where(StepStateTable.sessionId, SqlBuilder.isEqualTo(row.sessionId))
                .and(StepStateTable.stepName, SqlBuilder.isEqualTo(row.stepName))
                .and(StepStateTable.stepKey, SqlBuilder.isEqualTo(row.stepKey));

        int affectedRows = extensions.update(updateStatement);
        if (affectedRows == 0) {
            Buildable<GeneralInsertModel> insertStatement = GeneralInsertDSL.insertInto(StepStateTable.TABLE)
                    .set(StepStateTable.sessionId).toValue(row.sessionId)
                    .set(StepStateTable.stepName).toValue(row.stepName)
                    .set(StepStateTable.stepKey).toValue(row.stepKey)
                    .set(StepStateTable.state).toValue(row.state)
                    .set(StepStateTable.startTime).toValueWhenPresent(row.startTime)
                    .set(StepStateTable.endTime).toValueWhenPresent(row.endTime)
                    .set(StepStateTable.executions).toValueWhenPresent(row.executions)
                    .set(StepStateTable.returnValue).toValueWhenPresent(row.returnValue)
                    .set(StepStateTable.parameters).toValueWhenPresent(row.parameters)
                    .set(StepStateTable.errorType).toValueWhenPresent(row.errorType)
                    .set(StepStateTable.exception).toValueWhenPresent(row.exception)
                    .set(StepStateTable.userVarStr).toValueWhenPresent(row.userVarStr)
                    .set(StepStateTable.userVarInt).toValueWhenPresent(row.userVarInt);

            extensions.generalInsert(insertStatement);
        }
    }

    @Override
    public StepStateItem load(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        StepStateRow row = loadState(sessionId, stepMethod.getName(), stepKey);
        if (row != null) {
            StepStateItem stepItem = new StepStateItem();
            stepItem.executionTimes = row.executions != null ? row.executions : 0;
            stepItem.returnValue = null;
            stepItem.parameters = new Object[0];
            stepItem.exception = null;

            if (StringUtils.isNotBlank(row.startTime)) {
                stepItem.startTime = Instant.parse(row.startTime);
            }

            if (StringUtils.isNotBlank(row.endTime)) {
                stepItem.endTime = Instant.parse(row.endTime);
            }

            if (StringUtils.isNotBlank(row.returnValue)) {
                try {
                    stepItem.returnValue = mapper.readValue(row.returnValue, stepMethod.getReturnType());
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step result", e);
                }
            }

            if (StringUtils.isNotBlank(row.parameters)) {
                try {
                    JsonNode jsonTree = mapper.readTree(row.parameters);
                    Class<?>[] paramTypes = stepMethod.getParameterTypes();
                    stepItem.parameters = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        JsonNode param = jsonTree.get(i);
                        stepItem.parameters[i] = mapper.treeToValue(param, paramTypes[i]);
                    }
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step parameters", e);
                }
            }

            if (StringUtils.isNotBlank(row.exception) && StringUtils.isNotBlank(row.errorType)) {
                try {
                    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                    Class<?> errorType = Class.forName(row.errorType, true, threadClassLoader);
                    stepItem.exception = (Throwable) mapper.readValue(row.exception, errorType);
                } catch (JsonProcessingException e) {
                    throw new StepStateException("Failed to decode step error", e);
                } catch (ClassNotFoundException e) {
                    throw new StepStateException("Error type not found", e);
                }
            }

            stepItem.state = StepState.valueOf(row.state);
            stepItem.userVarStr = row.userVarStr;
            stepItem.userVarInt = row.userVarInt;
            return stepItem;
        }

        return null;
    }

    @Override
    public void save(String sessionId, Method stepMethod, String stepKey, StepStateItem stepItem) throws Throwable {
        StepStateRow row = new StepStateRow();
        row.sessionId = sessionId;

        try {
            row.returnValue = mapper.writeValueAsString(stepItem.returnValue);
            System.out.println("ret: " + row.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step result", e);
        }

        try {
            row.parameters = mapper.writeValueAsString(stepItem.parameters);
            System.out.println("args: " + row.parameters);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step parameters", e);
        }

        try {
            if (stepItem.exception != null) {
                row.errorType = stepItem.exception.getClass().getName();
                row.exception = errorMapper.writeValueAsString(stepItem.exception);
                System.out.println("err: " + row.exception);
            }
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step error", e);
        }

        row.stepKey = stepKey;
        row.stepName = stepMethod.getName();
        row.state = stepItem.state.name();
        row.executions = stepItem.executionTimes;
        row.userVarStr = stepItem.userVarStr;
        row.userVarInt = stepItem.userVarInt;
        row.startTime = stepItem.startTime.toString();

        if (stepItem.endTime != null) {
            row.endTime = stepItem.endTime.toString();
        }

        saveState(row);
    }
}
