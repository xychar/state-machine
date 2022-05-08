package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.exception.SchedulingException;
import com.xychar.stateful.store.WorkflowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

public class WorkflowWorker extends Thread {
    final Logger logger = LoggerFactory.getLogger(WorkflowWorker.class);

    public String workerName;
    public WorkflowItem workflowItem;
    public WorkflowStore workflowStore;

    public final Method stepMethod;
    public final WorkflowInstance<?> instance;

    public Throwable lastError;

    public WorkflowWorker(WorkflowInstance<?> instance, Method stepMethod) {
        super("Workflow-" + instance.workerName);
        this.instance = instance;
        this.stepMethod = stepMethod;
        this.workerName = instance.workerName;
    }

    private long handleScheduling(SchedulingException e) {
        Instant currentTime = Instant.now();
        Instant nextRun = currentTime.plusMillis(e.waitingTime);
        workflowItem.lastRun = currentTime;
        workflowItem.nextRun = nextRun;

        return e.waitingTime;
    }

    private void executeWorkflowMethod() throws Throwable {
        while (!isInterrupted()) {
            try {
                Object result = stepMethod.invoke(instance);
                logger.info("Workflow result: " + result);
                break;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof SchedulingException) {
                    SchedulingException se = (SchedulingException) e.getTargetException();
                    long waitingTime = handleScheduling(se);
                    Thread.sleep(waitingTime);
                } else {
                    lastError = e.getTargetException();
                    logger.error("Workflow failed", lastError);
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            MDC.put("worker_name", workerName);
            logger.info("Worker started: " + workerName);

            executeWorkflowMethod();
        } catch (Throwable e) {
            lastError = e;
            e.printStackTrace();
        }
    }

    public void shutdown(long milliseconds) {
        try {
            interrupt();
            join(milliseconds);
        } catch (InterruptedException e) {
            logger.error("Failed to shutdown the worker");
        }
    }
}
