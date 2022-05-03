package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.exception.SchedulingException;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

@Component
public class WorkflowThread extends Thread {
    public String workerName;
    public WorkflowItem workflowItem;

    public final Method stepMethod;
    public final WorkflowInstance<?> instance;

    public Throwable lastError;

    public WorkflowThread(WorkflowInstance<?> instance, Method stepMethod) {
        this.instance = instance;
        this.stepMethod = stepMethod;
    }

    private void dispatchWorkflow() throws Throwable {
        while (!isInterrupted()) {
            try {
                Object result = stepMethod.invoke(instance);
                System.out.println("Done - step " + stepMethod.getName() + ", returned:" + result);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof SchedulingException) {
                    SchedulingException se = (SchedulingException) e.getTargetException();
                    Thread.sleep(se.waitingTime);

//                    long currentTime = Instant.now().toEpochMilli();
//                    long nextRun = currentTime + se.waitingTime;
                } else {
                    lastError = e.getTargetException();
                    e.getTargetException().printStackTrace();
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
