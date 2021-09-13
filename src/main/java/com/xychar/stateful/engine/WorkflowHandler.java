package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class WorkflowHandler implements Interceptor {

    private final WorkflowMetadata<?> metadata;
    public final WorkflowSessionBase<?> session;

    private final ObjectMapper mapper = new ObjectMapper();

    static final Map<String, StepState> cache = new HashMap<>();

    public WorkflowHandler(WorkflowMetadata<?> metadata, WorkflowSessionBase<?> session) {
        this.metadata = metadata;
        this.session = session;
    }

    public Object invoke(Object self, Callable<?> invocation, Method method, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = args.length > 0 ? args[0].toString() : "";
        String cacheKey = method.getName() + ":" + stepKey;

        StepState stepState = cache.get(cacheKey);
        if (stepState != null) {
            System.out.format("found method-call [%s] in cache", cacheKey);
            System.out.println();

            return stepState.result;
        }

        stepState = new StepState();
        stepState.args = args;
        stepState.result = invocation.call();

        String argsData = mapper.writeValueAsString(stepState.args);
        System.out.println("args: " + argsData);

        String retData = mapper.writeValueAsString(stepState.result);
        System.out.println("ret: " + retData);

        cache.put(cacheKey, stepState);
        return stepState.result;
    }

    @Override
    public Object intercept(WorkflowSessionBase<?> session, Callable<?> superCall,
                            Method method, Object... args) throws Throwable {
        return invoke(session, superCall, method, args);
    }
}
