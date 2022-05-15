package com.xychar.stateful.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.xychar.stateful.common.Utils;
import com.xychar.stateful.engine.ServiceLocator;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowStatus;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowDriver implements ApplicationContextAware, ServiceLocator {
    final Logger logger = LoggerFactory.getLogger(WorkflowDriver.class);

    public static final String settingsFile = "settings.json";
    public static final String benchmarkFile = "benchmark.json";
    private final ConfigLoader configs = new ConfigLoader();

    public ApplicationContext applicationContext;

    private final StepStateStore stepStateStore;
    private final WorkflowStore workflowStore;

    private final WorkflowEngine workflowEngine;

    public Class<?> workflowClass = null;
    public String methodName = null;
    public String sessionId = null;

    public WorkflowDriver(@Autowired StepStateStore stepStateStore,
                          @Autowired WorkflowStore workflowStore) {
        this.stepStateStore = stepStateStore;
        this.workflowStore = workflowStore;

        this.workflowEngine = new WorkflowEngine();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T lookup(Class<T> serviceClazz, String name) {
        if (name != null && !name.isEmpty()) {
            return applicationContext.getBean(name, serviceClazz);
        } else {
            return applicationContext.getBean(serviceClazz);
        }
    }

    @PostConstruct
    public void initialize() throws IOException {
        stepStateStore.createTableIfNotExists();
        workflowStore.createTableIfNotExists();

        workflowEngine.stateAccessor = this.stepStateStore;
        workflowEngine.serviceContainer = this;

        configs.loadRootConfig(new File(settingsFile));
        configs.loadUserConfig(new File(benchmarkFile));
    }

    public void execute() throws Exception {
        Method stepMethod = Utils.getStepMethod(workflowClass, methodName);
        WorkflowMetadata<?> metadata = workflowEngine.buildFrom(workflowClass);

        Map<String, JsonNode> configList = this.configs.getMergedConfigs();

        final List<WorkflowWorker> workers = new ArrayList<>();
        for (Map.Entry<String, JsonNode> cfg : configList.entrySet()) {
            WorkflowInstance<?> instance = workflowEngine.newInstance(metadata);
            instance.inputObject = cfg.getValue();
            instance.workerName = cfg.getKey();

            WorkflowWorker thread = new WorkflowWorker(instance, stepMethod);
            thread.workerName = instance.workerName;

            thread.workflowItem = workflowStore.loadSimple(sessionId, thread.workerName);
            if (thread.workflowItem == null) {
                thread.workflowItem = workflowStore.createFrom(stepMethod);
                thread.workflowItem.workerName = thread.workerName;
                thread.workflowItem.sessionId = sessionId;
                thread.workflowItem.status = WorkflowStatus.EXECUTING;
                thread.workflowItem.startTime = Instant.now();
                workflowStore.save(thread.workflowItem);
            }

            instance.setExecutionId(thread.workflowItem.executionId);
            workers.add(thread);
            thread.start();
        }

        Thread jvmHook = new ShutdownHook(workers);
        Runtime.getRuntime().addShutdownHook(jvmHook);

        // Waiting for all threads to end
        for (WorkflowWorker thread : workers) {
            thread.join();
        }

        workers.clear();
        Runtime.getRuntime().removeShutdownHook(jvmHook);
    }

    class ShutdownHook extends Thread {
        private final List<WorkflowWorker> workers;

        public ShutdownHook(List<WorkflowWorker> workers) {
            this.workers = workers;
        }

        @Override
        public void run() {
            logger.info("Shutting down workflow engine");

            // Waiting for all threads to end
            for (WorkflowWorker thread : workers) {
                thread.shutdown(0);
            }

            // Waiting for all threads to end
            for (WorkflowWorker thread : workers) {
                thread.shutdown(15000);
            }
        }
    }
}
