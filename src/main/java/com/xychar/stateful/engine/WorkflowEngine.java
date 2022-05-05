package com.xychar.stateful.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xychar.stateful.exception.WorkflowException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.DeclaringTypeResolver;
import net.bytebuddy.implementation.bind.MethodNameEqualityResolver;
import net.bytebuddy.implementation.bind.ParameterLengthResolver;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowEngine implements ServiceContainer {

    public StepStateAccessor stateAccessor;

    public ServiceContainer serviceContainer;

    private final ObjectMapper mapper = new ObjectMapper();

    private ByteBuddy newByteBuddy(Class<?> clazz) {
        return new ByteBuddy().with(
                new NamingStrategy.SuffixingRandom(
                        "Stateful",
                        new NamingStrategy.Suffixing.BaseNameResolver.ForGivenType(
                                new TypeDescription.ForLoadedType(clazz)
                        )
                )
        );
    }

    /**
     * Only intercept interface methods
     */
    private ElementMatcher<ByteCodeElement> methodFilter(Class<?> clazz) {
        return ElementMatchers.isDeclaredBy(
                ElementMatchers.isSuperTypeOf(clazz)
                        .and(ElementMatchers.isInterface())
        );
    }

    public Constructor<? extends OutputProxy> buildOutputProxy(Class<?> outputClazz) {
        ClassLoader classLoader = outputClazz.getClassLoader();

        DynamicType.Unloaded<OutputProxy> dynamicType = newByteBuddy(outputClazz)
                .subclass(OutputProxy.class)
                .method(methodFilter(outputClazz))
                .intercept(MethodDelegation.withEmptyConfiguration()
                        .withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS)
                        .withResolvers(
                                MethodNameEqualityResolver.INSTANCE,
                                ParameterLengthResolver.INSTANCE,
                                ArgumentTypeResolver.INSTANCE,
                                DeclaringTypeResolver.INSTANCE,
                                BindingPriority.Resolver.INSTANCE)
                        .toField("handler"))
                .make();

        Class<? extends OutputProxy> outputProxyClass = dynamicType.load(classLoader).getLoaded();

        try {
            return outputProxyClass.getConstructor();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to locate the default constructor", e);
        }
    }

    public <T> WorkflowMetadata<T> buildFrom(Class<T> workflowClazz) {
        ClassLoader classLoader = workflowClazz.getClassLoader();

        WorkflowMetadata<T> metadata = new WorkflowMetadata<>();
        metadata.workflowClass = workflowClazz;

        DynamicType.Unloaded<WorkflowInstance> dynamicType = newByteBuddy(workflowClazz)
                .subclass(WorkflowInstance.class)
                .implement(workflowClazz)
                .method(methodFilter(workflowClazz))
                .intercept(MethodDelegation.withEmptyConfiguration()
                        .withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS)
                        .withBinders(StepKeyArgs.Binder.INSTANCE, MethodKind.Binder.INSTANCE)
                        .withResolvers(
                                MethodNameEqualityResolver.INSTANCE,
                                ParameterLengthResolver.INSTANCE,
                                ArgumentTypeResolver.INSTANCE,
                                DeclaringTypeResolver.INSTANCE,
                                BindingPriority.Resolver.INSTANCE)
                        .toField("handler"))
                .make();

        metadata.workflowProxyClass = dynamicType.load(classLoader).getLoaded();

        try {
            metadata.workflowConstructor = metadata.workflowProxyClass.getConstructor();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to locate the default constructor", e);
        }

        // Build output proxy classes
        Map<Class<?>, Constructor<? extends OutputProxy>> outputProxies = new LinkedHashMap<>();
        for (Method m : workflowClazz.getMethods()) {
            if (m.getDeclaringClass().isInterface() && m.getAnnotation(Output.class) != null) {
                outputProxies.put(m.getReturnType(), buildOutputProxy(m.getReturnType()));
            }
        }

        metadata.outputCreators = outputProxies;
        return metadata;
    }

    public <T> WorkflowInstance<T> newInstance(WorkflowMetadata<T> metadata) {
        WorkflowInstance<T> instance = metadata.newInstance();
        instance.handler = new WorkflowHandler(metadata, instance, stateAccessor);
        return instance;
    }

    @Override
    public <T> T lookupService(Class<T> serviceClazz, String name) {
        if (serviceContainer != null) {
            return serviceContainer.lookupService(serviceClazz, name);
        }

        return null;
    }
}
