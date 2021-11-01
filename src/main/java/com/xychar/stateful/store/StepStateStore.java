package com.xychar.stateful.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.engine.StepStateData;
import com.xychar.stateful.engine.StepStateException;
import com.xychar.stateful.engine.WorkflowException;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.GeneralInsertDSL;
import org.mybatis.dynamic.sql.insert.GeneralInsertModel;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.Buildable;
import org.mybatis.dynamic.sql.util.spring.NamedParameterJdbcTemplateExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

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

    public <T> T selectList(Buildable<SelectModel> selectStatement, ResultSetExtractor<T> rse) {
        SelectStatementProvider statementProvider = selectStatement.build()
                .render(RenderingStrategies.SPRING_NAMED_PARAMETER);
        return template.query(statementProvider.getSelectStatement(),
                statementProvider.getParameters(), rse);
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
                    .set(StepStateTable.exception).toValueWhenPresent(row.exception);

            extensions.generalInsert(insertStatement);
        }
    }

    @Override
    public StepStateData load(String sessionId, Method stepMethod, String stepKey) throws Throwable {
        StepStateRow row = loadState(sessionId, stepMethod.getName(), stepKey);
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

    @Override
    public void save(String sessionId, Method stepMethod, String stepKey, StepStateData stateData) throws Throwable {
        StepStateRow row = new StepStateRow();
        row.sessionId = sessionId;

        try {
            row.returnValue = mapper.writeValueAsString(stateData.returnValue);
            System.out.println("ret: " + row.returnValue);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step result", e);
        }

        try {
            row.parameters = mapper.writeValueAsString(stateData.parameters);
            System.out.println("args: " + row.parameters);
        } catch (JsonProcessingException e) {
            throw new StepStateException("Failed to encode step parameters", e);
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

        row.stepKey = stepKey;
        row.stepName = stepMethod.getName();
        row.state = stateData.state.name();
        row.executions = stateData.executionTimes;
        row.startTime = stateData.startTime.toString();

        if (stateData.endTime != null) {
            row.endTime = stateData.endTime.toString();
        }

        saveState(row);
    }

    static abstract class ThrowableMixIn {
        @JsonIgnore
        abstract Throwable getCause();

        @JsonIgnore
        abstract StackTraceElement[] getStackTrace();

        @JsonIgnore
        abstract String getLocalizedMessage();

        @JsonIgnore
        abstract Throwable[] getSuppressed();
    }
}
