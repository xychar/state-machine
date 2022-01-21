package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepKey;
import com.xychar.stateful.engine.StepState;
import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.SubStep;
import com.xychar.stateful.engine.Workflow;
import org.apache.commons.lang3.Validate;

@Workflow
public interface BenchmarkEc2 {
    @SubStep
    default String launchEc2() {
        System.out.println("*** Method [launchEc2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", Steps.getExecutionTimes());
        return "i-001";
    }

    /**
     * Non-stateful method to query step state.
     */
    default boolean isEc2Launched() {
        Steps.query(this).launchEc2();
        StepState state = Steps.getStepStateOfLastCall();
        return StepState.Done.equals(state);
    }

    @SubStep
    default void checkEc2(@StepKey String ec2Id) {
        System.out.println("*** Method [checkEc2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", Steps.getExecutionTimes());
        Validate.isTrue(Steps.getExecutionTimes() >= 5, "Retrying ...");
    }

    @SubStep
    default void pingSsm(String ec2Id) {
        System.out.println("*** Method [pingSsm] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", Steps.getExecutionTimes());
    }

    @SubStep
    default void installHdb(String ec2Id) {
        System.out.println("*** Method [installHdb] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", Steps.getExecutionTimes());
    }

    @Step
    default String ec2() {
        System.out.println("*** Method [ec2] executed in BenchmarkEc2");
        System.out.format("Step execution times: %d%n", Steps.getExecutionTimes());

        String ec2Id = launchEc2();
        checkEc2(ec2Id);
        pingSsm(ec2Id);
        installHdb(ec2Id);

        return ec2Id;
    }
}
