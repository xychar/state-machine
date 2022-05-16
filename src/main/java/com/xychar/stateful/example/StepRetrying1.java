package com.xychar.stateful.example;

import com.xychar.stateful.engine.Retry;
import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.SubStep;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface StepRetrying1 {
    Logger logger = LoggerFactory.getLogger(StepRetrying1.class);

    @SubStep
    @Retry(maxAttempts = 6, intervalSeconds = 5, succeedAfterRetrying = true)
    default String step1() {
        logger.info("Executing step1");
        logger.info("Current execution times of step1: {}", Steps.getExecutionTimes());

        logger.info("Retry current step, until max-attempts is reached");
        Steps.retry("Retry-" + Steps.getExecutionTimes());
        return "i-001";
    }

    @Startup
    default String step2() {
        logger.info("Executing step2");
        logger.info("Current execution times of step2: {}", Steps.getExecutionTimes());

        String ret = step1();
        logger.info("Result of step1: {}", ret);

        Steps.succeed("done-done");
        return ret;
    }
}
