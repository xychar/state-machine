package com.xychar.stateful.spring;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface WorkflowBase1 {
    @Step
    default void hello(String t1) {
        System.out.println("*** Method ${::hello.name} executed in WorkflowBase1");

        welcome(t1);
    }

    @Step
    default void welcome(String t1) {
        System.out.println("*** Method ${::welcome.name} executed in WorkflowBase1");
    }
}
