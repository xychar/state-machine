package com.xychar.stateful.example;

import com.xychar.stateful.engine.Retry;
import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.engine.StepChecker;
import com.xychar.stateful.engine.StepKey;
import com.xychar.stateful.engine.StepStatus;
import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.SubStep;
import com.xychar.stateful.engine.Workflow;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface BenchmarkEc2 {
    Logger logger = LoggerFactory.getLogger(BenchmarkEc2.class);

    @SubStep
    default String launchEc2() {
        logger.info("Launch EC2 instance ...");
        return "i-001";
    }

    /**
     * Non-stateful method to query step state.
     */
    default boolean isEc2Launched() {
        logger.info("Checking if step has been executed");
        Steps.query(BenchmarkEc2.class).launchEc2();
        StepStatus status = Steps.getStepStatusOfLastQuery();

        logger.info("Step status of launchEc2: {}", status);
        return StepStatus.DONE.equals(status);
    }

    @SubStep
    @Retry(intervalSeconds = 5)
    default void checkEc2(@StepKey String ec2Id) {
        logger.info("Checking if EC2 instance is available ...");
        logger.info("Current execution times of checkEc2: {}", Steps.getExecutionTimes());
        Validate.isTrue(Steps.getExecutionTimes() >= 5, "Retrying [checkEc2] ...");
    }

    @SubStep
    default void pingSsm(String ec2Id) {
        logger.info("Ping if SSM is reachable ...");
    }

    @SubStep
    default void installHdb(String ec2Id) {
        logger.info("Installing HDB on EC2 instance: {}", ec2Id);
    }

    @Startup
    default String ec2() {
        logger.info("Building EC2 instance ...");
        logger.info("Current execution times of ec2: {}", Steps.getExecutionTimes());

        String ec2Id = launchEc2();
        checkEc2(ec2Id);
        pingSsm(ec2Id);
        installHdb(ec2Id);

        StepStatus launchEc2Status = StepChecker.status(BenchmarkEc2::launchEc2);
        logger.info("Step status of launchEc2: {}", launchEc2Status);
        StepStatus pingSsmStatus = StepChecker.status(BenchmarkEc2::pingSsm, "ec2-id");
        logger.info("Step status of pingSsm: {}", pingSsmStatus);

        return ec2Id;
    }
}
