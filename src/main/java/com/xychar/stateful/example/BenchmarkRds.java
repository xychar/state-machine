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
    default DbEndpoint checkRds(String rdsId) {
        System.out.println("*** Method [checkRds] executed in BenchmarkRds");
        DbEndpoint endpoint = new DbEndpoint();
        endpoint.host = "host-01";
        endpoint.port = 1024;
        return endpoint;
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
    default void testDb(DbEndpoint ep) {
        System.out.println("*** Method [testDb] executed in BenchmarkRds");
        System.out.format("*** testDb, host=%s, port=%d%n", ep.host, ep.port);
    }

    @Step
    private void hello() {
        System.out.println("*** Private method [hello] executed in BenchmarkRds");
    }

    @Step
    default String rds() {
        hello();
        System.out.println("*** Method [rds] executed in BenchmarkRds");

        String rdsId = createRds();
        checkRds(rdsId);

        String host = dbHost(rdsId);
        int port = dbPort(rdsId);
        pingDb(host, port);
        createDb(host, port);

        DbEndpoint ep = checkRds(rdsId);
        testDb(ep);
        return rdsId;
    }
}
