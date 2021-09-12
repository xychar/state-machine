package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class StepInterceptor implements Interceptor {

    private final WorkflowMetadata<?> metadata;
    public final WorkflowSessionBase<?> session;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, MethodCall> cache = new HashMap<>();

    public StepInterceptor(WorkflowMetadata<?> metadata, WorkflowSessionBase<?> session) {
        this.metadata = metadata;
        this.session = session;
    }

    public Object invoke(Callable<?> superCall, Method method, Object[] args) throws Throwable {
        System.out.println("*** invoking method: " + method.getName());

        String stepKey = args.length > 0 ? args[0].toString() : "";
        String cacheKey = method.getName() + ":" + stepKey;

        MethodCall lastCall = cache.get(cacheKey);
        if (lastCall != null) {
            System.out.format("found method-call [%s] in cache", cacheKey);
            System.out.println();

            return lastCall.result;
        }

        lastCall = new MethodCall();
        lastCall.args = args;
        lastCall.result = superCall.call();

        String argsData = mapper.writeValueAsString(lastCall.args);
        System.out.println("args: " + argsData);

        String retData = mapper.writeValueAsString(lastCall.result);
        System.out.println("ret: " + retData);

        cache.put(cacheKey, lastCall);
        return lastCall.result;
    }

    @RuntimeType
    public static Object delegate(@This WorkflowSessionBase<?> session,
                                  @DefaultCall Callable<?> superCall,
                                  @Origin Method method,
                                  @AllArguments Object[] args) throws Throwable {
        return session.delegate.invoke(superCall, method, args);
    }

    static class MethodCall {
        public Object result;
        public Object[] args;
    }
}
