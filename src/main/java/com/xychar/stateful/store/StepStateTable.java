package com.xychar.stateful.store;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StepStateTable extends SqlTable {
    public static final SqlTable TABLE = new StepStateTable();

    public static final SqlColumn<String> sessionId = TABLE.column("session_id", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepName = TABLE.column("step_name", JDBCType.VARCHAR);
    public static final SqlColumn<String> stepKey = TABLE.column("step_key", JDBCType.VARCHAR);
    public static final SqlColumn<String> state = TABLE.column("state", JDBCType.VARCHAR);
    public static final SqlColumn<String> exception = TABLE.column("exception", JDBCType.VARCHAR);
    public static final SqlColumn<String> errorType = TABLE.column("error_type", JDBCType.VARCHAR);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> parameters = TABLE.column("parameters", JDBCType.VARCHAR);
    public static final SqlColumn<String> returnValue = TABLE.column("return_value", JDBCType.VARCHAR);

    protected StepStateTable() {
        super("t_step_state");
    }

    public static StepStateRow mappingAllColumns(ResultSet rs, int index) throws SQLException {
        StepStateRow record = new StepStateRow();

        record.sessionId = rs.getString(sessionId.name());
        record.stepName = rs.getString(stepName.name());
        record.stepKey = rs.getString(stepKey.name());
        record.state = rs.getString(state.name());
        record.exception = rs.getString(exception.name());
        record.errorType = rs.getString(errorType.name());
        record.startTime = rs.getString(startTime.name());
        record.endTime = rs.getString(endTime.name());
        record.parameters = rs.getString(parameters.name());
        record.returnValue = rs.getString(returnValue.name());

        return record;
    }
}
