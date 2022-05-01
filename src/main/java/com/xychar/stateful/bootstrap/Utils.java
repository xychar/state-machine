package com.xychar.stateful.bootstrap;

import com.xychar.stateful.exception.SchedulingException;
import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowStore;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {

    public static void executeDynamic(AbstractApplicationContext context, Class<?> workflowClazz,
                                      String methodName, String sessionId) throws Exception {
        StepStateStore stepStateStore = context.getBean(StepStateStore.class);
        stepStateStore.createTableIfNotExists();

        WorkflowStore workflowStore = context.getBean(WorkflowStore.class);
        workflowStore.createTableIfNotExists();

        WorkflowEngine engine = new WorkflowEngine();
        engine.stateAccessor = stepStateStore;

        WorkflowMetadata<?> metadata = engine.buildFrom(workflowClazz);
        WorkflowInstance<?> session = engine.newWorkflowInstance(metadata);

        session.setExecutionId(sessionId);
        Object workflow = session.getWorkflowInstance();

        Method method = workflowClazz.getMethod(methodName);

        for (int i = 0; i < 200; i++) {
            try {
                Object result = method.invoke(workflow);
                System.out.println(methodName + ": " + result);
                break;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof SchedulingException) {
                    SchedulingException se = (SchedulingException) e.getTargetException();

                    if (se.waitingTime >= 200) {
                        Thread.sleep(se.waitingTime);
                    } else {
                        Thread.sleep(5000L);
                    }

                    continue;
                }

                throw e;
            }
        }
    }
}
