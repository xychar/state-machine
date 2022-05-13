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

@Workflow
public interface BenchmarkEc2 {
    @SubStep
    default String launchEc2() {
        System.out.println("*** Method [launchEc2] executed in BenchmarkEc2");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
        return "i-001";
    }

    /**
     * Non-stateful method to query step state.
     */
    default boolean isEc2Launched() {
        Steps.query(this).launchEc2();
        StepStatus status = Steps.getStepStatusOfLastQuery();
        return StepStatus.DONE.equals(status);
    }

    @SubStep
    @Retry(intervalSeconds = 5)
    default void checkEc2(@StepKey String ec2Id) {
        System.out.println("*** Method [checkEc2] executed in BenchmarkEc2");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
        Validate.isTrue(Steps.getExecutionTimes() >= 5, "Retrying ...");
    }

    @SubStep
    default void pingSsm(String ec2Id) {
        System.out.println("*** Method [pingSsm] executed in BenchmarkEc2");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
    }

    @SubStep
    default void installHdb(String ec2Id) {
        System.out.println("*** Method [installHdb] executed in BenchmarkEc2");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());
    }

    @Startup
    default String ec2() {
        System.out.println("*** Method [ec2] executed in BenchmarkEc2");
        System.out.format("*** Step execution times: %d%n", Steps.getExecutionTimes());

        String ec2Id = launchEc2();
        checkEc2(ec2Id);
        pingSsm(ec2Id);
        installHdb(ec2Id);

        StepChecker.check1(BenchmarkEc2::launchEc2);
        StepChecker.check1(BenchmarkEc2::pingSsm, "ec2-id");

        return ec2Id;
    }
}
