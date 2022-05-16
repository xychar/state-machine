package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface BenchmarkRun extends BenchmarkVpc, BenchmarkEc2, BenchmarkRds {
    Logger logger = LoggerFactory.getLogger(BenchmarkRun.class);

    @Step
    default void installSoftware(String ec2Id) {
        logger.info("Install software");
    }

    @Step
    default void uploadConfigs(String ec2Id) {
        logger.info("Upload configs to: {}", ec2Id);
    }

    @Step
    default void prepare(String ec2Id) {
        logger.info("Prepare test: {}", ec2Id);
    }

    @Step
    default void start(String ec2Id) {
        logger.info("Start test: {}", ec2Id);
    }

    @Step
    default void check(String ec2Id) {
        logger.info("Check test: {}", ec2Id);
    }

    @Step
    default void finish(String ec2Id) {
        logger.info("Finish up test: {}", ec2Id);
    }

    @Step
    default void execute(String ec2Id) {
        logger.info("Execute test: {}", ec2Id);

        network();
        ec2();
        rds();

        installSoftware(ec2Id);
        uploadConfigs(ec2Id);

        start(ec2Id);
        check(ec2Id);
        finish(ec2Id);
    }

    @Step
    default void main() {
        logger.info("Main entry");
    }
}
