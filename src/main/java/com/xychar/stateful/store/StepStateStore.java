package com.xychar.stateful.store;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.insert.GeneralInsertDSL;
import org.mybatis.dynamic.sql.insert.GeneralInsertModel;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.util.Buildable;
import org.mybatis.dynamic.sql.util.spring.NamedParameterJdbcTemplateExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StepStateStore {
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
                "  last_error varchar(2000) NULL,",
                "  start_time varchar(50) NULL,",
                "  end_time varchar(50) NULL,",
                "  PRIMARY KEY(session_id, step_name, step_key)",
                ")"
        ));
    }

    public StepStateRecord loadState(String sessionId, String stepName, String stepKey) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        Buildable<SelectModel> selectStatement = SelectDSL.select(StepStateTable.TABLE.allColumns())
                .from(StepStateTable.TABLE)
                .where(StepStateTable.sessionId, SqlBuilder.isEqualTo(sessionId))
                .and(StepStateTable.stepName, SqlBuilder.isEqualTo(stepName))
                .and(StepStateTable.stepKey, SqlBuilder.isEqualTo(stepKey));

        return extensions.selectOne(selectStatement, StepStateTable::mappingAllColumns).orElse(null);
    }

    public void saveState(StepStateRecord row) {
        NamedParameterJdbcTemplateExtensions extensions = new NamedParameterJdbcTemplateExtensions(template);

        UpdateDSL.UpdateWhereBuilder updateStatement = UpdateDSL.update(StepStateTable.TABLE)
                .set(StepStateTable.state).equalToWhenPresent(row.state)
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
                    .set(StepStateTable.endTime).toValueWhenPresent(row.endTime);

            extensions.generalInsert(insertStatement);
        }
    }
}
