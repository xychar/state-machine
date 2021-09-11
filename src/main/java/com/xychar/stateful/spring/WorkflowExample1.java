package com.xychar.stateful.spring;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

import java.io.Serializable;

@Workflow
public interface WorkflowExample1 extends WorkflowBase1, Serializable {

    @Step
    @Override
    default void hello(String t1) {
        System.out.println("*** Method ${::hello.name} executed in WorkflowExample1");
        WorkflowBase1.super.hello(t1);
    }

    @Step
    default void init() {
        System.out.println("*** Method ${::init.name} executed");
    }

    @Step
    default String input() {
        System.out.println("*** Method ${::input.name} executed");
        return "A";
    }

    @Step
    default void optionA() {
        System.out.println("*** Method ${::optionA.name} executed");
    }

    @Step
    default void optionB() {
        System.out.println("*** Method ${::optionB.name} executed");
    }

    @Step
    default String example1() {
        init();

        String data = input();
        if (data.equals("A")) {
            optionA();
        } else {
            optionB();
        }

        hello("ab");
        return data;
    }
}
