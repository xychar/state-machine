package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workflow
public interface WorkflowBase1 {
    Logger logger = LoggerFactory.getLogger(WorkflowBase1.class);

    @Step
    default void hello(String t1) {
        logger.info("We are in hello: {}", t1);
        welcome(t1);
    }

    @Step
    default void welcome(String t1) {
        logger.info("We are in welcome: {}", t1);
    }
}
