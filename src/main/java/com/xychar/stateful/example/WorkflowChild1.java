package com.xychar.stateful.example;

import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepKey;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Random;

@Workflow
public interface WorkflowChild1 extends WorkflowBase1, WorkflowBase2 {
    Logger logger = LoggerFactory.getLogger(WorkflowChild1.class);

    Random RAND = new SecureRandom();

    @Step
    @Override
    default void hello(@StepKey String t1) {
        logger.info("Hello, call step in base workflow");
        WorkflowBase1.super.hello(t1);
    }

    @Step
    @Override
    default void welcome(@StepKey String t1) {
        logger.info("Welcome, call step in base workflow");
        WorkflowBase1.super.welcome(t1);
        WorkflowBase2.super.welcome(t1);
    }

    @Step
    default void init() {
        logger.info("Initialize");
    }

    @Step
    default String input() {
        logger.info("Get user input - random");
        String[] buff = {"A", "B"};

        return buff[RAND.nextInt() & 1];
    }

    @Step
    default void optionA() {
        logger.info("Option A is chosen");
    }

    @Step
    default void optionB() {
        logger.info("Option B is chosen");
    }

    @Step
    default void hi(@StepKey String a, int b, String c, @StepKey int d) {
        logger.info("Hi, test step key: {}, {}", a, d);
    }

    @Startup
    default Integer example1() {
        logger.info("We are now in example1");
        init();

        String data = input();
        if (data.equals("A")) {
            optionA();
        } else {
            optionB();
        }

        hello("ab");

        hi("p1", 'b', "c", 'd');
        hi("p1", 'b', "c", 'f');
        hi("p2", 'b', "c", 'd');
        return Integer.parseInt(data.toLowerCase(), 16);
    }
}
