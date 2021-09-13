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
    public static final SqlColumn<String> lastError = TABLE.column("last_error", JDBCType.VARCHAR);
    public static final SqlColumn<String> startTime = TABLE.column("start_time", JDBCType.VARCHAR);
    public static final SqlColumn<String> endTime = TABLE.column("end_time", JDBCType.VARCHAR);

    protected StepStateTable() {
        super("t_step_state");
    }

    public static StepStateRecord mappingAllColumns(ResultSet rs, int index) throws SQLException {
        StepStateRecord record = new StepStateRecord();

        record.sessionId = rs.getString(sessionId.name());
        record.stepName = rs.getString(stepName.name());
        record.stepKey = rs.getString(stepKey.name());
        record.state = rs.getString(state.name());
        record.lastError = rs.getString(lastError.name());
        record.startTime = rs.getString(startTime.name());
        record.endTime = rs.getString(endTime.name());

        return record;
    }
}
