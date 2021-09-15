package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class WorkflowHandler implements Interceptor {

    private final WorkflowMetadata<?> metadata;
    private final WorkflowSessionBase<?> session;
    private final StepStateAccessor accessor;

    private final ObjectMapper mapper = new ObjectMapper();

    static final Map<String, StepStateData> cache = new HashMap<>();

    public WorkflowHandler(WorkflowMetadata<?> metadata,
                           WorkflowSessionBase<?> session,
                           StepStateAccessor accessor) {
        this.metadata = metadata;
        this.session = session;
        this.accessor = accessor;
    }

    public Object invoke(Object self, Callable<?> invocation, Method method, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = args.length > 0 ? args[0].toString() : "_";
        StepStateData stateData = accessor.load(session.sessionId, method.getName(), stepKey);
        if (stateData != null) {
            System.out.format("found method-call [%s:%s] in cache", method.getName(), stepKey);
            System.out.println();

            return stateData.returnValue;
        }

        Object result = invocation.call();
        stateData = new StepStateData();
        stateData.parameters = mapper.writeValueAsString(args);
        System.out.println("args: " + stateData.parameters);

        stateData.returnValue = mapper.writeValueAsString(result);
        System.out.println("ret: " + stateData.returnValue);

        accessor.save(session.sessionId, method.getName(), stepKey, stateData);
        return stateData.returnValue;
    }

    @Override
    public Object intercept(WorkflowSessionBase<?> session, Callable<?> superCall,
                            Method method, Object... args) throws Throwable {
        return invoke(session, superCall, method, args);
    }
}
