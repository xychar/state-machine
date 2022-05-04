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
            "  execution_id varchar(50) NOT NULL,",
            "  step_name varchar(200) NOT NULL,",
            "  step_key varchar(200) NOT NULL,",
            "  status varchar(20) NOT NULL,",
            "  message varchar(2000) NULL,",
            "  executions integer NULL,",
            "  start_time varchar(50) NULL,",
            "  end_time varchar(50) NULL,",
            "  return_value text NULL,",
            "  parameters text NULL,",
            "  error_type varchar(200) NULL,",
            "  exception text NULL,",
            "  last_result text NULL,",
            "  user_var_int integer NULL,",
            "  user_var_str varchar(200) NULL,",
            "  user_var_obj text NULL,",
            "  PRIMARY KEY(execution_id, step_name, step_key)",
            ")"
    );

    public static final SqlTable TABLE = new StepStateTable();

    public static final SqlColumn<String> executionId = TABLE.column("execution_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepName = TABLE.column("step_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepKey = TABLE.column("step_key", JDBCType.VARCHAR);
    public static final SqlColumn<String> status = TABLE.column("status", JDBCType.VARCHAR);
    public static final SqlColumn<String> message = TABLE.column("message", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> executions = TABLE.column("executions", JDBCType.INTEGER);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);
    public static final SqlColumn<String> parameters = TABLE.column("parameters", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);
    public static final SqlColumn<String> lastResult = TABLE.column("last_result", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> userVarInt = TABLE.column("user_var_int", JDBCType.INTEGER);
    public static final SqlColumn<String> userVarStr = TABLE.column("user_var_str", JDBCType.VARCHAR);
    public static final SqlColumn<String> userVarObj = TABLE.column("user_var_obj", JDBCType.VARCHAR);

    protected StepStateTable() {
        super("t_step_state");
    }

    public static StepStateRow mappingAllColumns(ResultSet rs, int rowNum) throws SQLException {
        StepStateRow row = new StepStateRow();

        row.executionId = rs.getString(executionId.name());
        row.stepName = rs.getString(stepName.name());
        row.stepKey = rs.getString(stepKey.name());
        row.status = rs.getString(status.name());
        row.message = rs.getString(message.name());

        row.executions = rs.getInt(executions.name());
        row.startTime = rs.getString(startTime.name());
        row.endTime = rs.getString(endTime.name());

        row.returnValue = rs.getString(returnValue.name());
        row.parameters = rs.getString(parameters.name());
        row.errorType = rs.getString(errorType.name());
        row.exception = rs.getString(exception.name());
        row.lastResult = rs.getString(lastResult.name());

        row.userVarInt = rs.getInt(userVarInt.name());
        row.userVarStr = rs.getString(userVarStr.name());
        row.userVarObj = rs.getString(userVarObj.name());

        return row;
    }

    public static ResultSetExtractor<List<StepStateRow>> resultSetExtractor() {
        DataExtractor<StepStateRow> extractor = new DataExtractor<>(StepStateRow.class);

        extractor.mapColumnToProperty(executionId.name(), "executionId");
        extractor.mapColumnToProperty(stepName.name(), "stepName");
        extractor.mapColumnToProperty(stepKey.name(), "stepKey");
        extractor.mapColumnToProperty(status.name(), "status");
        extractor.mapColumnToProperty(message.name(), "message");
        extractor.mapColumnToProperty(executions.name(), "executions");
        extractor.mapColumnToProperty(startTime.name(), "startTime");
        extractor.mapColumnToProperty(endTime.name(), "endTime");
        extractor.mapColumnToProperty(returnValue.name(), "returnValue");
        extractor.mapColumnToProperty(parameters.name(), "parameters");
        extractor.mapColumnToProperty(errorType.name(), "errorType");
        extractor.mapColumnToProperty(exception.name(), "exception");
        extractor.mapColumnToProperty(lastResult.name(), "lastResult");
        extractor.mapColumnToProperty(userVarInt.name(), "userVarInt");
        extractor.mapColumnToProperty(userVarStr.name(), "userVarStr");
        extractor.mapColumnToProperty(userVarObj.name(), "userVarObj");

        return extractor;
    }
}
