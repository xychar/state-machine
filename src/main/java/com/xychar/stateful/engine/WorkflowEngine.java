package com.xychar.stateful.engine;

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

public class WorkflowEngine {

    public StepStateAccessor stateAccessor;

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
        metadata.stateAccessor = this.stateAccessor;
        metadata.workflowClass = workflowClazz;

        DynamicType.Unloaded<WorkflowSessionBase> dynamicType = newByteBuddy(workflowClazz)
                .subclass(WorkflowSessionBase.class)
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

//                .intercept(MethodDelegation.withDefaultConfiguration()
//                        .withBinders(StepKeyArgs.Binder.INSTANCE)
//                        .toField("handler"))
                .make();

        metadata.workflowProxyClass = dynamicType.load(classLoader).getLoaded();
        return metadata;
    }

}
