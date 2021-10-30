package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepOperations;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface BenchmarkRds extends StepOperations {
    @Step
    default String createRds() {
        System.out.println("*** Method [createRds] executed in BenchmarkRds");
        return "rds-001";
    }

    @Step
    default void checkRds(String rdsId) {
        System.out.println("*** Method [checkRds] executed in BenchmarkRds");
    }

    @Step
    default String dbHost(String rdsId) {
        System.out.println("*** Method [dbHost] executed in BenchmarkRds");
        return "host-name";
    }

    @Step
    default int dbPort(String rdsId) {
        System.out.println("*** Method [dbPort] executed in BenchmarkRds");
        return 1520;
    }

    @Step
    default void pingDb(String host, int port) {
        System.out.println("*** Method [pingDb] executed in BenchmarkRds");
    }

    @Step
    default void createDb(String host, int port) {
        System.out.println("*** Method [createDb] executed in BenchmarkRds");
    }

    @Step
    default String rds() {
        System.out.println("*** Method [rds] executed in BenchmarkRds");

        String rdsId = createRds();
        checkRds(rdsId);

        String host = dbHost(rdsId);
        int port = dbPort(rdsId);
        pingDb(host, port);
        createDb(host, port);

        return rdsId;
    }
}
