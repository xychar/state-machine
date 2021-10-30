package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepKey;
import com.xychar.stateful.engine.StepOperations;
import com.xychar.stateful.engine.Workflow;
import org.apache.commons.lang3.Validate;

@Workflow
public interface BenchmarkEc2 extends StepOperations {
    @Step
    default String launchEc2() {
        System.out.println("*** Method [launchEc2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", getExecutionTimes());
        return "i-001";
    }

    @Step
    default void checkEc2(@StepKey String ec2Id) {
        System.out.println("*** Method [checkEc2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", getExecutionTimes());
        Validate.isTrue(getExecutionTimes() >= 5, "Retrying ...");
    }

    @Step
    default void pingSsm(String ec2Id) {
        System.out.println("*** Method [pingSsm] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", getExecutionTimes());
    }

    @Step
    default void installHdb(String ec2Id) {
        System.out.println("*** Method [installHdb] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", getExecutionTimes());
    }

    @Step
    default String ec2() {
        System.out.println("*** Method [ec2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", getExecutionTimes());

        String ec2Id = launchEc2();
        checkEc2(ec2Id);
        pingSsm(ec2Id);
        installHdb(ec2Id);

        return ec2Id;
    }
}
