package com.xychar.stateful.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

public class WorkflowEngine {

    public StepStateAccessor stateAccessor;

    private final ObjectMapper mapper = new ObjectMapper();

    private ByteBuddy newByteBuddy(Class<?> clazz) {
        return new ByteBuddy().with(
                new NamingStrategy.SuffixingRandom(
                        "Stateful",
                        new NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType(
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
                        .withBinders(StepKeyArgs.Binder.INSTANCE)
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

        return metadata;
    }

    public <T> WorkflowInstance<T> newWorkflowInstance(WorkflowMetadata<T> metadata) {
        WorkflowInstance<T> instance = metadata.newInstance();
        instance.handler = new WorkflowHandler(metadata, instance, stateAccessor);
        return instance;
    }

    public WorkflowItem buildWorkflow(String className, String methodName, String params) {
        WorkflowItem item = new WorkflowItem();
        item.className = className;
        item.methodName = methodName;

        try {
            Object[] parameters = new Object[0];
            JsonNode jsonTree = mapper.createArrayNode();
            if (StringUtils.isNotBlank(params)) {
                jsonTree = mapper.readTree(params);
                parameters = new Object[jsonTree.size()];
            }

            ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
            Class<?> workflowClass = Class.forName(className, true, threadClassLoader);

            for (Method m : workflowClass.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == parameters.length) {
                    item.stepMethod = m;
                    break;
                }
            }

            if (item.stepMethod == null) {
                throw new WorkflowException("Failed to locate step method: " + methodName);
            }

            Class<?>[] paramTypes = item.stepMethod.getParameterTypes();
            parameters = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                JsonNode param = jsonTree.get(i);
                parameters[i] = mapper.treeToValue(param, paramTypes[i]);
            }

            item.parameters = parameters;
            return item;
        } catch (JsonProcessingException e) {
            throw new WorkflowException("Failed to build workflow step", e);
        } catch (ClassNotFoundException e) {
            throw new WorkflowException("Workflow class not found", e);
        }
    }
}
