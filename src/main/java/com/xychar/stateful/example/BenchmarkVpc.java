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
}
