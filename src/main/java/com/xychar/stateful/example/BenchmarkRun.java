package com.xychar.stateful.example;

import com.xychar.stateful.engine.Step;
import com.xychar.stateful.engine.StepOperations;
import com.xychar.stateful.engine.Workflow;

@Workflow
public interface BenchmarkRun extends BenchmarkVpc, BenchmarkEc2, BenchmarkRds, StepOperations {

}
