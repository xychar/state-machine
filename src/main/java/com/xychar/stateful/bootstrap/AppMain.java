package com.xychar.stateful.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowSession;
import com.xychar.stateful.example.WorkflowChild1;
import com.xychar.stateful.spring.AppConfig;
import com.xychar.stateful.spring.Exceptions;
import com.xychar.stateful.store.StepStateRow;
import com.xychar.stateful.store.StepStateStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class AppMain {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void executeExample(AbstractApplicationContext context) throws Exception {
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

        StepStateStore store = context.getBean(StepStateStore.class);
        store.createTableIfNotExists();

        StepStateRow row = store.loadState("s01", "hello", "sk1");
        if (row == null) {
            row = new StepStateRow();
            row.sessionId = "s01";
            row.stepName = "hello";
            row.stepKey = "sk1";
            row.state = "pending";
            store.saveState(row);
        }

        row = store.loadState("s01", "hello", "sk1");
        System.out.println(mapper.writeValueAsString(row));
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(Exceptions.class);
        context.register(AppConfig.class);

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
