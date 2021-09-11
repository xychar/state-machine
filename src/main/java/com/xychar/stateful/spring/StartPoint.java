package com.xychar.stateful.spring;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowSession;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.Serializable;

public class StartPoint {

    public static void execute() {
        WorkflowEngine engine = new WorkflowEngine();
        WorkflowMetadata<WorkflowExample1> metadata = engine.buildFrom(WorkflowExample1.class);
        WorkflowSession<WorkflowExample1> session = metadata.newSession();
        WorkflowExample1 workflow = session.getWorkflowInstance();

        workflow.example1();
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ExceptionConfig.class);
        context.register(AppConfig.class);

        context.refresh();
        context.start();

        try {
            execute();
            System.out.println("Hello");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.stop();
            context.close();
        }
    }
}
