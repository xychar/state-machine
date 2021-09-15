package com.xychar.stateful.store;

import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.StepStateAccessor;
import com.xychar.stateful.engine.StepStateData;
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

@Component
public class StepStateStore implements StepStateAccessor {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate template;

    public StepStateStore(@Autowired JdbcTemplate jdbcTemplate,
                          @Autowired NamedParameterJdbcTemplate template) {
        this.jdbcTemplate = jdbcTemplate;
        this.template = template;
    }

    public void createTableIfNotExists() {
        jdbcTemplate.execute(String.join("\n",
                "CREATE TABLE IF NOT EXISTS t_step_state(",
                "  session_id varchar(50) NOT NULL,",
                "  step_name varchar(200) NOT NULL,",
                "  step_key varchar(200) NOT NULL,",
                "  state varchar(20) NOT NULL,",
                "  last_error text NULL,",
                "  start_time varchar(50) NULL,",
                "  end_time varchar(50) NULL,",
                "  parameters text NULL,",
                "  return_value text NULL,",
                "  PRIMARY KEY(session_id, step_name, step_key)",
                ")"
        ));
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
                .set(StepStateTable.lastError).equalToWhenPresent(row.lastError)
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
                    .set(StepStateTable.lastError).toValueWhenPresent(row.lastError)
                    .set(StepStateTable.startTime).toValueWhenPresent(row.startTime)
                    .set(StepStateTable.endTime).toValueWhenPresent(row.endTime)
                    .set(StepStateTable.parameters).toValueWhenPresent(row.parameters)
                    .set(StepStateTable.returnValue).toValueWhenPresent(row.returnValue);

            extensions.generalInsert(insertStatement);
        }
    }

    @Override
    public StepStateData load(String sessionId, String stepName, String stepKey) {
        StepStateRow row = loadState(sessionId, stepName, stepKey);
        if (row != null) {
            StepStateData stateData = new StepStateData();
            stateData.parameters = row.parameters;
            stateData.returnValue = row.returnValue;
            return stateData;
        }

        return null;
    }

    @Override
    public void save(String sessionId, String stepName, String stepKey, StepStateData stateData) {
        StepStateRow row = new StepStateRow();
        row.sessionId = sessionId;
        row.stepName = stepName;
        row.stepKey = stepKey;
        row.state = StepState.Done.name();
        row.parameters = stateData.parameters;
        row.returnValue = stateData.returnValue;
        saveState(row);
    }
}
