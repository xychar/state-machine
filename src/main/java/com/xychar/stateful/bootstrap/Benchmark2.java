package com.xychar.stateful.bootstrap;

import com.xychar.stateful.example.BenchmarkRds;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

public class Benchmark2 {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class, AppConfig.class);

        context.refresh();
        context.start();

        try {
            String sessionId = UUID.randomUUID().toString();
            Utils.executeDynamic(context, BenchmarkRds.class, "rds", sessionId);

            System.out.println("=== Rerun Benchmark workflow.");
            Utils.executeDynamic(context, BenchmarkRds.class, "rds", sessionId);

            System.out.println("=== Benchmark finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
