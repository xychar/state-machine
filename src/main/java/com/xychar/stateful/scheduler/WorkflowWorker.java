package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.exception.SchedulingException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

public class WorkflowWorker extends Thread {
    public String workerName;
    public WorkflowItem workflowItem;

    public final Method stepMethod;
    public final WorkflowInstance<?> instance;

    public Throwable lastError;

    public WorkflowWorker(WorkflowInstance<?> instance, Method stepMethod) {
        this.instance = instance;
        this.stepMethod = stepMethod;
    }

    private long handleScheduling(SchedulingException e) {
        Instant currentTime = Instant.now();
        Instant nextRun = currentTime.plusMillis(e.waitingTime);
        workflowItem.lastRun = currentTime;
        workflowItem.nextRun = nextRun;

        return e.waitingTime;
    }

    private void dispatchWorkflow() throws Throwable {
        while (!isInterrupted()) {
            try {
                Object result = stepMethod.invoke(instance);
                System.out.println("Workflow result: " + result);
                break;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof SchedulingException) {
                    SchedulingException se = (SchedulingException) e.getTargetException();
                    long waitingTime = handleScheduling(se);
                    Thread.sleep(waitingTime);
                } else {
                    lastError = e.getTargetException();
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            dispatchWorkflow();
        } catch (Throwable e) {
            lastError = e;
            e.printStackTrace();
        }
    }
}
