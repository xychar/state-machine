package com.xychar.stateful.example;

import com.xychar.stateful.engine.Retry;
import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.SubStep;
import com.xychar.stateful.engine.Workflow;
import org.apache.commons.lang3.Validate;

@Workflow
public interface StepRetrying2 {
    @SubStep
    @Retry(maxAttempts = 6, intervalSeconds = 5, succeedAfterRetrying = true)
    default String step1() {
        System.out.println("*** Method [step1] executed in StepRetryingTest");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
        Validate.isTrue(Steps.getExecutionTimes() >= 20, "Retrying ...");
        return "i-001";
    }

    @Startup
    default String step2() {
        System.out.println("*** Method [step1] executed in StepRetryingTest");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());

        String ret = step1();
        System.out.format("*** Result of step1: %s%n", ret);

        Steps.succeed("Done-done");
        return ret;
    }
}
