package com.xychar.stateful.example;

import com.xychar.stateful.engine.Retry;
import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.SubStep;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface StepRetrying1 {
    @SubStep
    default String step1() {
        System.out.println("*** Method [step1] executed in StepRetryingTest");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
        Steps.succeed("Success-1");
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
