package com.xychar.stateful.store;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WorkflowTable extends SqlTable {
    public static final String CREATE_TABLE = String.join("\n",
            "CREATE TABLE IF NOT EXISTS t_workflow(",
            "  session_id varchar(50) NOT NULL,",
            "  class_name varchar(200) NOT NULL,",
            "  method_name varchar(200) NOT NULL,",
            "  step_key varchar(200) NOT NULL,",
            "  state varchar(20) NOT NULL,",
            "  executions integer NOT NULL,",
            "  start_time varchar(50) NULL,",
            "  end_time varchar(50) NULL,",
            "  next_run integer NULL,",
            "  last_run integer NULL,",
            "  return_value text NULL,",
            "  parameters text NULL,",
            "  error_type varchar(200) NULL,",
            "  exception text NULL,",
            "  PRIMARY KEY(session_id, class_name, method_name, step_key)",
            ")"
    );

    public static final SqlTable TABLE = new WorkflowTable();

    public static final SqlColumn<String> sessionId = TABLE.column("session_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> className = TABLE.column("class_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> methodName = TABLE.column("method_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepKey = TABLE.column("step_key", JDBCType.VARCHAR);
    public static final SqlColumn<String> state = TABLE.column("state", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> executions = TABLE.column("executions", JDBCType.INTEGER);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<Long> nextRun = TABLE.column("next_run", JDBCType.INTEGER);
    public static final SqlColumn<Long> lastRun = TABLE.column("last_run", JDBCType.INTEGER);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);
    public static final SqlColumn<String> parameters = TABLE.column("parameters", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);

    protected WorkflowTable() {
        super("t_step_state");
    }

    public static WorkflowRow mappingAllColumns(ResultSet rs, int rowNum) throws SQLException {
        WorkflowRow row = new WorkflowRow();

        row.sessionId = rs.getString(sessionId.name());
        row.className = rs.getString(className.name());
        row.methodName = rs.getString(methodName.name());
        row.stepKey = rs.getString(stepKey.name());
        row.state = rs.getString(state.name());

        row.executions = rs.getInt(executions.name());
        row.startTime = rs.getString(startTime.name());
        row.endTime = rs.getString(endTime.name());
        row.nextRun = rs.getLong(nextRun.name());
        row.lastRun = rs.getLong(lastRun.name());

        row.returnValue = rs.getString(returnValue.name());
        row.parameters = rs.getString(parameters.name());
        row.errorType = rs.getString(errorType.name());
        row.exception = rs.getString(exception.name());

        return row;
    }

    public static ResultSetExtractor<List<WorkflowRow>> resultSetExtractor() {
        DataExtractor<WorkflowRow> extractor = new DataExtractor<>(WorkflowRow.class);

        extractor.mapColumnToProperty(sessionId.name(), "sessionId");
        extractor.mapColumnToProperty(className.name(), "className");
        extractor.mapColumnToProperty(methodName.name(), "methodName");
        extractor.mapColumnToProperty(stepKey.name(), "stepKey");
        extractor.mapColumnToProperty(state.name(), "state");

        extractor.mapColumnToProperty(executions.name(), "executions");
        extractor.mapColumnToProperty(startTime.name(), "startTime");
        extractor.mapColumnToProperty(endTime.name(), "endTime");
        extractor.mapColumnToProperty(nextRun.name(), "nextRun");
        extractor.mapColumnToProperty(lastRun.name(), "lastRun");

        extractor.mapColumnToProperty(returnValue.name(), "returnValue");
        extractor.mapColumnToProperty(parameters.name(), "parameters");
        extractor.mapColumnToProperty(errorType.name(), "errorType");
        extractor.mapColumnToProperty(exception.name(), "exception");

        return extractor;
    }
}
