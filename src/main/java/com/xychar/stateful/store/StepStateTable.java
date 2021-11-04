package com.xychar.stateful.store;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StepStateTable extends SqlTable {
    public static final String CREATE_TABLE = String.join("\n",
            "CREATE TABLE IF NOT EXISTS t_step_state(",
            "  session_id varchar(50) NOT NULL,",
            "  step_name varchar(200) NOT NULL,",
            "  step_key varchar(200) NOT NULL,",
            "  state varchar(20) NOT NULL,",
            "  executions integer NOT NULL,",
            "  start_time varchar(50) NULL,",
            "  end_time varchar(50) NULL,",
            "  return_value text NULL,",
            "  parameters text NULL,",
            "  error_type varchar(200) NULL,",
            "  exception text NULL,",
            "  PRIMARY KEY(session_id, step_name, step_key)",
            ")"
    );

    public static final SqlTable TABLE = new StepStateTable();

    public static final SqlColumn<String> sessionId = TABLE.column("session_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepName = TABLE.column("step_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepKey = TABLE.column("step_key", JDBCType.VARCHAR);
    public static final SqlColumn<String> state = TABLE.column("state", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> executions = TABLE.column("executions", JDBCType.INTEGER);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);
    public static final SqlColumn<String> parameters = TABLE.column("parameters", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);

    protected StepStateTable() {
        super("t_step_state");
    }

    public static StepStateRow mappingAllColumns(ResultSet rs, int rowNum) throws SQLException {
        StepStateRow row = new StepStateRow();

        row.sessionId = rs.getString(sessionId.name());
        row.stepName = rs.getString(stepName.name());
        row.stepKey = rs.getString(stepKey.name());
        row.state = rs.getString(state.name());

        row.executions = rs.getInt(executions.name());
        row.startTime = rs.getString(startTime.name());
        row.endTime = rs.getString(endTime.name());

        row.returnValue = rs.getString(returnValue.name());
        row.parameters = rs.getString(parameters.name());
        row.errorType = rs.getString(errorType.name());
        row.exception = rs.getString(exception.name());

        return row;
    }

    public static ResultSetExtractor<List<StepStateRow>> resultSetExtractor() {
        DataExtractor<StepStateRow> extractor = new DataExtractor<>(StepStateRow.class);

        extractor.mapColumnToProperty(sessionId.name(), "sessionId");
        extractor.mapColumnToProperty(stepName.name(), "stepName");
        extractor.mapColumnToProperty(stepKey.name(), "stepKey");
        extractor.mapColumnToProperty(state.name(), "state");
        extractor.mapColumnToProperty(executions.name(), "executions");
        extractor.mapColumnToProperty(startTime.name(), "startTime");
        extractor.mapColumnToProperty(endTime.name(), "endTime");
        extractor.mapColumnToProperty(returnValue.name(), "returnValue");
        extractor.mapColumnToProperty(parameters.name(), "parameters");
        extractor.mapColumnToProperty(errorType.name(), "errorType");
        extractor.mapColumnToProperty(exception.name(), "exception");

        return extractor;
    }
}
