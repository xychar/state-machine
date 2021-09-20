package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepKey;
import com.xychar.stateful.engine.Workflow;
import com.xychar.stateful.engine.WorkflowOperations;

import java.security.SecureRandom;
import java.util.Random;

@Workflow
public interface WorkflowChild1 extends WorkflowBase1, WorkflowBase2, WorkflowOperations {

    static final Random RAND = new SecureRandom();

    @Step
    @Override
    default void hello(@StepKey String t1) {
        System.out.println("*** Method [hello] executed in WorkflowChild1");
        WorkflowBase1.super.hello(t1);
    }

    @Step
    @Override
    default void welcome(@StepKey String t1) {
        WorkflowBase1.super.welcome(t1);
        WorkflowBase2.super.welcome(t1);
    }

    @Step
    default void init() {
        System.out.println("*** Method [init] executed in WorkflowChild1");
    }

    @Step
    default String input() {
        System.out.println("*** Method [input] executed in WorkflowChild1");
        String[] buff = {"A", "B"};

        return buff[RAND.nextInt() & 1];
    }

    @Step
    default void optionA() {
        System.out.println("*** Method [optionA] executed in WorkflowChild1");
    }

    @Step
    default void optionB() {
        System.out.println("*** Method [optionB] executed in WorkflowChild1");
    }

    @Step
    default void hi(@StepKey String a, int b, String c, @StepKey int d) {
        System.out.println("*** Method [hi] executed in WorkflowChild1");
    }

    void sleep(long milliseconds);

    void waitFor(long milliseconds);

    @Step
    default Integer example1() {
        System.out.println("*** Method [example1] executed in WorkflowChild1");
        init();

        String data = input();
        if (data.equals("A")) {
            optionA();
        } else {
            optionB();
        }

        hello("ab");

        sleep(3000L);
        waitFor(8000L);

        hi("p1", 'b', "c", 'd');
        return Integer.parseInt(data.toLowerCase(), 16);
    }
}