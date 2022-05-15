package com.xychar.stateful.mybatis;

import org.apache.ibatis.annotations.Update;

public interface StepStateMapper {
    int createTableIfNotExists();

    StepStateRow load(String executionId, String stepName, String stepKey);

    int insert(StepStateRow row);

    int update(StepStateRow row);

    @Update({"<script>",
            "UPDATE t_step_state",
            "<set>",
            "    <if test='status != null'>",
            "        status = #{status},",
            "    </if>",
            "    <if test='message != null'>",
            "        message = #{message},",
            "    </if>",
            "    <if test='executions != null'>",
            "        executions = #{executions},",
            "    </if>",
            "    <if test='startTime != null'>",
            "        start_time = #{startTime},",
            "    </if>",
            "    <if test='endTime != null'>",
            "        end_time = #{endTime},",
            "    </if>",
            "    <if test='returnValue != null'>",
            "        return_value = #{returnValue},",
            "    </if>",
            "    <if test='parameters != null'>",
            "        parameters = #{parameters},",
            "    </if>",
            "    <if test='errorType != null'>",
            "        error_type = #{errorType},",
            "    </if>",
            "    <if test='exception != null'>",
            "        'exception' = #{exception},",
            "    </if>",
            "    <if test='lastResult != null'>",
            "        last_result = #{lastResult},",
            "    </if>",
            "    <if test='userVarInt != null'>",
            "        user_var_int = #{userVarInt},",
            "    </if>",
            "    <if test='userVarStr != null'>",
            "        user_var_str = #{userVarStr},",
            "    </if>",
            "    <if test='userVarObj != null'>",
            "        user_var_obj = #{userVarObj},",
            "    </if>",
            "</set>",
            "WHERE execution_id = #{executionId}",
            "AND step_name = #{stepName}",
            "AND step_key = #{stepKey}",
            "</script>"})
    int update2(StepStateRow row);
}
