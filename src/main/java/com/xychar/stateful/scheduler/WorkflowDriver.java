package com.xychar.stateful.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.xychar.stateful.engine.Startup;
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
import java.io.IOException;
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
    private final ConfigLoader configs = new ConfigLoader();

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
    public void initialize() throws IOException {
        stepStateStore.createTableIfNotExists();
        workflowStore.createTableIfNotExists();

        workflowEngine.stateAccessor = this.stepStateStore;

        configs.loadRootConfig(new File(settingsFile));
        configs.loadUserConfig(new File(benchmarkFile));
    }

    @PreDestroy
    public void destroy() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    /**
     * Locate the workflow entry method.
     */
    public Method getStepMethod(Class<?> workflowClazz, String methodName) {
        if (methodName != null && !methodName.isEmpty()) {
            try {
                // The step method should have no parameters
                return workflowClazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new StepNotFoundException("Step not found: " + methodName, e);
            }
        } else {
            // Search first startup step in the declared methods of the workflow class
            Optional<Method> found = Arrays.stream(workflowClazz.getDeclaredMethods())
                    .filter(x -> x.getAnnotation(Startup.class) != null)
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        }

        throw new StepNotFoundException("Default step not found");
    }

    public void execute() throws Exception {
        Method stepMethod = getStepMethod(workflowClass, null);
        WorkflowMetadata<?> metadata = workflowEngine.buildFrom(workflowClass);

        Map<String, JsonNode> tasks = configs.getMergedConfigs();
        List<WorkflowWorker> threads = new ArrayList<>();
        for (Map.Entry<String, JsonNode> task : tasks.entrySet()) {
            WorkflowInstance<?> instance = workflowEngine.newWorkflowInstance(metadata);
            instance.inputObject = task.getValue();

            WorkflowWorker thread = new WorkflowWorker(instance, stepMethod);
            thread.workerName = task.getKey();

            thread.workflowItem = workflowStore.load(sessionId, thread.workerName);
            if (thread.workflowItem == null) {
                thread.workflowItem = workflowStore.create(stepMethod);
                thread.workflowItem.sessionId = sessionId;
                thread.workflowItem.workerName = thread.workerName;
                workflowStore.save(thread.workflowItem);
            }

            instance.setExecutionId(thread.workflowItem.executionId);
            threads.add(thread);
            thread.start();
        }

        for (WorkflowWorker thread : threads) {
            thread.join();
        }
    }
}
