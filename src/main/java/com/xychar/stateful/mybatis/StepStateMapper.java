package com.xychar.stateful.mybatis;

public interface StepStateMapper {

    StepStateRow load(String executionId, String stepName, String stepKey);

    int insert(StepStateRow row);

    int update(StepStateRow row);
}
