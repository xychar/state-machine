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
            "  execution_id varchar(50) NOT NULL,",
            "  worker_name varchar(50) NOT NULL,",
            "  session_id varchar(50) NOT NULL,",
            "  class_name varchar(200) NULL,",
            "  method_name varchar(200) NULL,",
            "  step_key varchar(200) NULL,",
            "  status varchar(20) NOT NULL,",
            "  executions integer NULL,",
            "  start_time varchar(50) NULL,",
            "  end_time varchar(50) NULL,",
            "  next_run integer NULL,",
            "  last_run integer NULL,",
            "  return_value text NULL,",
            "  exception text NULL,",
            "  error_type varchar(200) NULL,",
            "  config_data text NULL,",
            "  PRIMARY KEY(execution_id)",
            ")"
    );

    public static final String CREATE_INDEX = String.join(" ",
            "CREATE UNIQUE INDEX IF NOT EXISTS",
            "idx_workflow_name on t_workflow",
            "(session_id, worker_name)"
    );

    public static final SqlTable TABLE = new WorkflowTable();

    public static final SqlColumn<String> executionId = TABLE.column("execution_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> workerName = TABLE.column("worker_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> sessionId = TABLE.column("session_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> className = TABLE.column("class_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> methodName = TABLE.column("method_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> status = TABLE.column("status", JDBCType.VARCHAR);
    public static final SqlColumn<Integer> executions = TABLE.column("executions", JDBCType.INTEGER);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<Long> nextRun = TABLE.column("next_run", JDBCType.INTEGER);
    public static final SqlColumn<Long> lastRun = TABLE.column("last_run", JDBCType.INTEGER);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);
    public static final SqlColumn<String> configData = TABLE.column("config_data", JDBCType.VARCHAR);

    protected WorkflowTable() {
        super("t_workflow");
    }

    public static WorkflowRow mappingAllColumns(ResultSet rs, int rowNum) throws SQLException {
        WorkflowRow row = new WorkflowRow();

        row.executionId = rs.getString(executionId.name());
        row.workerName = rs.getString(workerName.name());
        row.sessionId = rs.getString(sessionId.name());
        row.className = rs.getString(className.name());
        row.methodName = rs.getString(methodName.name());
        row.status = rs.getString(status.name());

        row.executions = rs.getInt(executions.name());
        row.startTime = rs.getString(startTime.name());
        row.endTime = rs.getString(endTime.name());
        row.nextRun = rs.getLong(nextRun.name());
        row.lastRun = rs.getLong(lastRun.name());

        row.returnValue = rs.getString(returnValue.name());
        row.errorType = rs.getString(errorType.name());
        row.exception = rs.getString(exception.name());
        row.configData = rs.getString(configData.name());

        return row;
    }

    public static ResultSetExtractor<List<WorkflowRow>> resultSetExtractor() {
        DataExtractor<WorkflowRow> extractor = new DataExtractor<>(WorkflowRow.class);

        extractor.mapColumnToProperty(executionId.name(), "executionId");
        extractor.mapColumnToProperty(workerName.name(), "workerName");
        extractor.mapColumnToProperty(sessionId.name(), "sessionId");
        extractor.mapColumnToProperty(className.name(), "className");
        extractor.mapColumnToProperty(methodName.name(), "methodName");
        extractor.mapColumnToProperty(status.name(), "status");

        extractor.mapColumnToProperty(executions.name(), "executions");
        extractor.mapColumnToProperty(startTime.name(), "startTime");
        extractor.mapColumnToProperty(endTime.name(), "endTime");
        extractor.mapColumnToProperty(nextRun.name(), "nextRun");
        extractor.mapColumnToProperty(lastRun.name(), "lastRun");

        extractor.mapColumnToProperty(returnValue.name(), "returnValue");
        extractor.mapColumnToProperty(errorType.name(), "errorType");
        extractor.mapColumnToProperty(exception.name(), "exception");
        extractor.mapColumnToProperty(configData.name(), "configData");

        return extractor;
    }
}
