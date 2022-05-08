package com.xychar.stateful.bootstrap;

import com.xychar.stateful.example.StepRetrying1;
import com.xychar.stateful.scheduler.WorkflowDriver;
import com.xychar.stateful.spring.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

public class DriverTest1 {
    static final Logger logger = LoggerFactory.getLogger(DriverTest1.class);

    public static void main(String[] args) {
        AbstractApplicationContext context = AppConfig.initialize();
        WorkflowDriver driver = context.getBean(WorkflowDriver.class);

        try {
            driver.sessionId = "f7c0bd63-e262-41d0-aeea-4374550e1f2a";
            logger.info("sessionId: " + driver.sessionId);
            driver.workflowClass = StepRetrying1.class;
            driver.execute();

            logger.info("Workflow finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
