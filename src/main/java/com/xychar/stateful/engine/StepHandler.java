package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class StepHandler implements InvocationHandler {
    static final Double javaClassVersion = Double.parseDouble(System.getProperty("java.class.version"));

    private final WorkflowMetadata<?> metadata;
    public final WorkflowSessionBase<?> session;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, MethodCall> cache = new HashMap<>();

    public StepHandler(WorkflowMetadata<?> metadata, WorkflowSessionBase<?> session) {
        this.metadata = metadata;
        this.session = session;
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (javaClassVersion.intValue() <= 152) {
            final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);

            final Class<?> clazz = method.getDeclaringClass();
            return constructor.newInstance(clazz)
                    .in(clazz)
                    .unreflectSpecial(method, clazz)
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        } else {
            return MethodHandles.lookup()
                    .findSpecial(
                            method.getDeclaringClass(),
                            method.getName(),
                            MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                            method.getDeclaringClass())
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        lastCall.result = doInvoke(proxy, method, args);

        String argsData = mapper.writeValueAsString(lastCall.args);
        System.out.println("args: " + argsData);

        String retData = mapper.writeValueAsString(lastCall.result);
        System.out.println("ret: " + retData);

        cache.put(cacheKey, lastCall);
        return lastCall.result;
    }

    static class MethodCall {
        public Object result;
        public Object[] args;
    }
}
