package com.xychar.stateful.bootstrap;

import com.xychar.stateful.engine.Steps;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.example.WorkflowChild1;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import com.xychar.stateful.store.StepStateStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.UUID;

public class Example1 {

    public static void executeExample(AbstractApplicationContext context) {
        StepStateStore store = context.getBean(StepStateStore.class);
        store.createTableIfNotExists();

        WorkflowEngine engine = new WorkflowEngine();
        engine.stateAccessor = store;

        WorkflowMetadata<WorkflowChild1> metadata = engine.buildFrom(WorkflowChild1.class);
        WorkflowInstance<WorkflowChild1> session = engine.newWorkflowInstance(metadata);

        session.setExecutionId(UUID.randomUUID().toString());
        WorkflowChild1 workflow = session.getWorkflowInstance();

        System.out.println("first-run: example1");
        Integer data1 = workflow.example1();
        System.out.println("data1: " + data1);

        System.out.println("Re-run: example1");
        Integer data2 = workflow.example1();
        System.out.println("data2: " + data2);

        System.out.println("Re-run: input");
        String input1 = workflow.input();
        System.out.println("input1: " + input1);

        String sessionId = Steps.getExecutionId(workflow);
        System.out.println("SessionId: " + sessionId);
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class, AppConfig.class);

        context.refresh();
        context.start();

        try {
            executeExample(context);

            System.out.println("Example finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
