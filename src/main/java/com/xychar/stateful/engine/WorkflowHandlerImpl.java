package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class WorkflowHandlerImpl implements WorkflowHandler {

    private final WorkflowMetadata<?> metadata;
    private final WorkflowSessionBase<?> session;
    private final StepStateAccessor accessor;

    private final ObjectMapper mapper = new ObjectMapper();

    static final Map<String, StepStateData> cache = new HashMap<>();

    public WorkflowHandlerImpl(WorkflowMetadata<?> metadata,
                               WorkflowSessionBase<?> session,
                               StepStateAccessor accessor) {
        this.metadata = metadata;
        this.session = session;
        this.accessor = accessor;
    }

    public Object invoke(Object self, Callable<?> invocation, Method method,
                         String stepKeyArgs, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = args.length > 0 ? args[0].toString() : "_";
        StepStateData stateData = accessor.load(session.sessionId, method.getName(), stepKey);
        if (stateData != null) {
            System.out.format("found method-call [%s:%s] in cache", method.getName(), stepKey);
            System.out.println();

            Object result = null;
            if (StringUtils.isNoneBlank(stateData.returnValue)) {
                result = mapper.readValue(stateData.returnValue, method.getReturnType());
            }

            if (stateData.state == StepState.Done) {
                return result;
            }
        }

        try {
            Object result = invocation.call();

            stateData = new StepStateData();
            stateData.parameters = mapper.writeValueAsString(args);
            System.out.println("args: " + stateData.parameters);

            stateData.returnValue = mapper.writeValueAsString(result);
            System.out.println("ret: " + stateData.returnValue);

            stateData.exception = "";
            stateData.state = StepState.Done;
            accessor.save(session.sessionId, method.getName(), stepKey, stateData);
            return result;
        } catch (Exception e) {
            stateData = new StepStateData();
            stateData.parameters = mapper.writeValueAsString(args);
            stateData.exception = mapper.writeValueAsString(e);
            stateData.returnValue = "";
            stateData.state = StepState.Retrying;
            stateData.nextRun = Instant.now().plusSeconds(30L);
            accessor.save(session.sessionId, method.getName(), stepKey, stateData);

            SchedulingException scheduling = new SchedulingException();
            scheduling.currentMethod = method;
            scheduling.stepStateData = stateData;
            throw scheduling;
        }
    }

    @Override
    public Object intercept(WorkflowSessionBase<?> session, Callable<?> invocation,
                            Method method, String stepKeyArgs,
                            Object... args) throws Throwable {
        return invoke(session, invocation, method, stepKeyArgs, args);
    }

    @Override
    public Object intercept(WorkflowSessionBase<?> session,
                            Method method, String stepKeyArgs,
                            Object... args) throws Throwable {
        System.out.println("*** invoking non-default method: " + method.getName());
        return null;
    }

    @Override
    public void sleep(long milliseconds) {
        System.out.println("*** sleep: " + milliseconds);
    }

    @Override
    public void waitFor(long milliseconds) {
        System.out.println("*** waitFor: " + milliseconds);
    }

    @Override
    public String getSessionId() {
        return session.sessionId;
    }
}
