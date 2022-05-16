package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface BenchmarkVpc {
    Logger logger = LoggerFactory.getLogger(BenchmarkVpc.class);

    @Step
    default String tgw(String vpcId) {
        logger.info("Setup transit gateway for: {}", vpcId);
        return "tgw-01";
    }

    @Step
    default void network() {
        logger.info("Configure network");
    }
}
