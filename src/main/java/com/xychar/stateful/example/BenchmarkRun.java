package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface BenchmarkRun extends BenchmarkVpc, BenchmarkEc2, BenchmarkRds {
    @Step
    default void installSoftware(String ec2Id) {
        System.out.println("*** Method [installSoftware] executed in BenchmarkRun");
    }

    @Step
    default void uploadConfigs(String ec2Id) {
        System.out.println("*** Method [uploadConfigs] executed in BenchmarkRun");
    }

    @Step
    default void prepare(String ec2Id) {
        System.out.println("*** Method [prepare] executed in BenchmarkRun");
    }

    @Step
    default void start(String ec2Id) {
        System.out.println("*** Method [start] executed in BenchmarkRun");
    }

    @Step
    default void check(String ec2Id) {
        System.out.println("*** Method [check] executed in BenchmarkRun");
    }

    @Step
    default void finish(String ec2Id) {
        System.out.println("*** Method [finish] executed in BenchmarkRun");
    }

    @Step
    default void execute(String ec2Id) {
        System.out.println("*** Method [execute] executed in BenchmarkRun");
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
        System.out.println("*** Method [main] executed in BenchmarkRun");
        String ec2Id = ec2();
        execute(ec2Id);
    }
}
