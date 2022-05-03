package com.xychar.stateful.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.xychar.stateful.engine.Main;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.exception.StepNotFoundException;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Component
public class WorkflowDriver {
    public static final String settingsFile = "settings.json";
    public static final String benchmarkFile = "benchmark.json";

    private final StepStateStore stepStateStore;
    private final WorkflowStore workflowStore;

    private final WorkflowEngine workflowEngine;

    private ExecutorService threadPool = null;

    public Class<?> workflowClass = null;
    public String sessionId = null;

    public WorkflowDriver(@Autowired StepStateStore stepStateStore,
                          @Autowired WorkflowStore workflowStore) {
        this.stepStateStore = stepStateStore;
        this.workflowStore = workflowStore;

        this.workflowEngine = new WorkflowEngine();

    }

    @PostConstruct
    public void initialize() {
        stepStateStore.createTableIfNotExists();
        workflowStore.createTableIfNotExists();

        workflowEngine.stateAccessor = this.stepStateStore;
    }

    @PreDestroy
    public void destroy() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    public Method getStepMethod(Class<?> workflowClazz, String methodName) {
        if (methodName != null && !methodName.isEmpty()) {
            try {
                return workflowClazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new StepNotFoundException("Default step not found: " + methodName, e);
            }
        } else {
            Optional<Method> found = Arrays.stream(workflowClazz.getDeclaredMethods())
                    .filter(x -> x.getAnnotation(Main.class) != null)
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        }

        throw new StepNotFoundException("Default step not found");
    }

    public void execute() throws Exception {
        ConfigLoader configs = new ConfigLoader();
        configs.loadRootConfig(new File(settingsFile));
        configs.loadUserConfig(new File(benchmarkFile));

        Method stepMethod = getStepMethod(workflowClass, null);
        WorkflowMetadata<?> metadata = workflowEngine.buildFrom(workflowClass);

        Map<String, JsonNode> tasks = configs.mergeConfigs();
        List<WorkflowThread> threads = new ArrayList<>();
        for (Map.Entry<String, JsonNode> task : tasks.entrySet()) {
            WorkflowInstance<?> instance = workflowEngine.newWorkflowInstance(metadata);
            instance.inputObject = task.getValue();
            WorkflowThread thread = new WorkflowThread(instance, stepMethod);
            thread.workerName = task.getKey();
            threads.add(thread);
            thread.start();
        }

        for (WorkflowThread thread : threads) {
            thread.join();
        }
    }
}
