package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface BenchmarkRds {
    Logger logger = LoggerFactory.getLogger(BenchmarkRds.class);

    @Step
    default String createRds() {
        logger.info("Creating RDS instance ...");
        return "rds-001";
    }

    @Step
    default DbEndpoint checkRds(String rdsId) {
        logger.info("Checking if RDS instance is available ...");
        DbEndpoint endpoint = new DbEndpoint();
        endpoint.host = "host-01";
        endpoint.port = 1024;
        return endpoint;
    }

    @Step
    default String dbHost(String rdsId) {
        logger.info("Get host name of RDS instance: {}", rdsId);
        return "host-name";
    }

    @Step
    default int dbPort(String rdsId) {
        logger.info("Get port number of RDS instance: {}", rdsId);
        return 1520;
    }

    @Step
    default void pingDb(String host, int port) {
        logger.info("Ping database: {}:{}", host, port);
    }

    @Step
    default void createDb(String host, int port) {
        logger.info("Create user db: {}:{}", host, port);
    }

    @Step
    default void testDb(DbEndpoint ep) {
        logger.info("Test user db: {}:{}", ep.host, ep.port);
    }

    @Step
    default void hello() {
        logger.info("Say hello");
    }

    @Step
    default String rds() {
        logger.info("Build RDS instance");

        hello();

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
