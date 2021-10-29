package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface BenchmarkEc2 {
    @Step
    default String launchEc2() {
        System.out.println("*** Method [launchEc2] executed in BenchmarkEc2");
        return "i-001";
    }

    @Step
    default void checkEc2(String ec2Id) {
        System.out.println("*** Method [checkEc2] executed in BenchmarkEc2");
    }

    @Step
    default void pingSsm(String ec2Id) {
        System.out.println("*** Method [pingSsm] executed in BenchmarkEc2");
    }

    @Step
    default void installHdb(String ec2Id) {
        System.out.println("*** Method [installHdb] executed in BenchmarkEc2");
    }

    @Step
    default String ec2() {
        System.out.println("*** Method [ec2] executed in BenchmarkEc2");

        String ec2Id = launchEc2();
        checkEc2(ec2Id);
        ssmPing(ec2Id);
        installHDB(ec2Id);

        return ec2Id;
    }
}
