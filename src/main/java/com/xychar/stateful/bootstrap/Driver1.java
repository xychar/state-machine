package com.xychar.stateful.bootstrap;

import com.xychar.stateful.example.BenchmarkEc2;
import com.xychar.stateful.scheduler.WorkflowDriver;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Driver1 {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class, AppConfig.class);

        context.refresh();
        context.start();

        WorkflowDriver driver = context.getBean(WorkflowDriver.class);

        try {
            // String sessionId = UUID.randomUUID().toString();
            driver.sessionId = "f7c0bd63-e262-41d0-aeea-4374550e1f2a";
            driver.workflowClass = BenchmarkEc2.class;
            driver.execute();
            System.out.println("=== Benchmark finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
