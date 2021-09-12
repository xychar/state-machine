package com.xychar.stateful.bootstrap;

import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowSession;
import com.xychar.stateful.example.WorkflowChild1;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AppMain {

    public static void executeExample() {
        WorkflowEngine engine = new WorkflowEngine();
        WorkflowMetadata<WorkflowChild1> metadata = engine.buildFrom(WorkflowChild1.class);
        WorkflowSession<WorkflowChild1> session = metadata.newSession();
        WorkflowChild1 workflow = session.getWorkflowInstance();

        System.out.println("first-run: example1");
        String data1 = workflow.example1();
        System.out.println("data1: " + data1);

        System.out.println("Re-run: example1");
        String data2 = workflow.example1();
        System.out.println("data2: " + data2);

        System.out.println("Re-run: input");
        String input1 = workflow.input();
        System.out.println("input1: " + input1);
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class);
        context.register(AppConfig.class);

        context.refresh();
        context.start();

        try {
            executeExample();

            System.out.println("Example finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
