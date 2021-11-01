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
            "  state varchar(20) NOT NULL,",
            "  executions integer NOT NULL,",
            "  start_time varchar(50) NULL,",
            "  end_time varchar(50) NULL,",
            "  next_run integer NULL,",
            "  last_run integer NULL,",
            "  return_value text NULL,",
            "  error_type varchar(200) NULL,",
            "  exception text NULL,",
            "  PRIMARY KEY(session_id, class_name, method_name)",
            ")"
    );

    public static final SqlTable TABLE = new WorkflowTable();

    public static final SqlColumn<String> sessionId = TABLE.column("session_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> className = TABLE.column("class_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> methodName = TABLE.column("method_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> state = TABLE.column("state", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> executions = TABLE.column("executions", JDBCType.INTEGER);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<Long> nextRun = TABLE.column("next_run", JDBCType.INTEGER);
    public static final SqlColumn<Long> lastRun = TABLE.column("last_run", JDBCType.INTEGER);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);

    protected WorkflowTable() {
        super("t_step_state");
    }

    public static WorkflowRow mappingAllColumns(ResultSet rs, int rowNum) throws SQLException {
        WorkflowRow record = new WorkflowRow();

        record.sessionId = rs.getString(sessionId.name());
        record.className = rs.getString(className.name());
        record.methodName = rs.getString(methodName.name());
        record.state = rs.getString(state.name());

        record.executions = rs.getInt(executions.name());
        record.startTime = rs.getString(startTime.name());
        record.endTime = rs.getString(endTime.name());
        record.nextRun = rs.getLong(nextRun.name());
        record.lastRun = rs.getLong(lastRun.name());

        record.returnValue = rs.getString(returnValue.name());
        record.errorType = rs.getString(errorType.name());
        record.exception = rs.getString(exception.name());

        return record;
    }

    public static ResultSetExtractor<List<WorkflowRow>> resultSetExtractor() {
        DataExtractor<WorkflowRow> extractor = new DataExtractor<>(WorkflowRow.class);

        extractor.mapColumnToProperty(sessionId.name(), "sessionId");
        extractor.mapColumnToProperty(className.name(), "className");
        extractor.mapColumnToProperty(methodName.name(), "methodName");
        extractor.mapColumnToProperty(state.name(), "state");
        extractor.mapColumnToProperty(executions.name(), "executions");
        extractor.mapColumnToProperty(startTime.name(), "startTime");
        extractor.mapColumnToProperty(endTime.name(), "endTime");
        extractor.mapColumnToProperty(nextRun.name(), "nextRun");
        extractor.mapColumnToProperty(lastRun.name(), "lastRun");
        extractor.mapColumnToProperty(returnValue.name(), "returnValue");
        extractor.mapColumnToProperty(errorType.name(), "errorType");
        extractor.mapColumnToProperty(exception.name(), "exception");

        return extractor;
    }
}
