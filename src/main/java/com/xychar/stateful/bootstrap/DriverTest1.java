package com.xychar.stateful.bootstrap;

import com.xychar.stateful.example.BenchmarkEc2;
import com.xychar.stateful.scheduler.WorkflowDriver;
import com.xychar.stateful.spring.AppConfig;
import org.springframework.context.support.AbstractApplicationContext;

public class DriverTest1 {

    public static void main(String[] args) {
        AbstractApplicationContext context = AppConfig.initialize();
        WorkflowDriver driver = context.getBean(WorkflowDriver.class);

        try {
            driver.sessionId = "f7c0bd63-e262-41d0-aeea-4374550e1f2a";
            System.out.println("=== Session Id: " + driver.sessionId);
            driver.workflowClass = BenchmarkEc2.class;
            driver.execute();

            System.out.println("=== Workflow finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
