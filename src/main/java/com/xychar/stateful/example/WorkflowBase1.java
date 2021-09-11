package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface WorkflowBase1 {
    @Step
    default void hello(String t1) {
        System.out.println("*** Method [hello] executed in WorkflowBase1");

        welcome(t1);
    }

    @Step
    default void welcome(String t1) {
        System.out.println("*** Method [welcome] executed in WorkflowBase1");
    }
}
