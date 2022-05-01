package com.xychar.stateful.bootstrap;

import com.xychar.stateful.example.BenchmarkEc2;
import com.xychar.stateful.example.BenchmarkRun;
import com.xychar.stateful.example.WorkflowChild1;
import com.xychar.stateful.scheduler.WorkflowScheduler;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;
import java.util.UUID;

public class Scheduler1 {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class, AppConfig.class);

        context.refresh();
        context.start();

        WorkflowScheduler scheduler = context.getBean(WorkflowScheduler.class);

        try {
            // String sessionId = UUID.randomUUID().toString();
            String sessionId = "f7c0bd63-e262-41d0-aeea-4374550e1f2a";
            scheduler.createWorkflowIfNotExists(sessionId, BenchmarkEc2.class, "ec2");

            scheduler.run();
            System.out.println("=== Benchmark finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
