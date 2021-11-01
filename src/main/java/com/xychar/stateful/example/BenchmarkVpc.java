package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface BenchmarkVpc {
    @Step
    default String tgw(String vpcId) {
        System.out.println("*** Method [tgw] executed in BenchmarkVpc");
        return "tgw-01";
    }

    @Step
    default void network() {
        System.out.println("*** Method [network] executed in BenchmarkVpc");
    }
}
