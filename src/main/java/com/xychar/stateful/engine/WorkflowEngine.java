package com.xychar.stateful.engine;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
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
        WorkflowMetadata<T> metadata = new WorkflowMetadata<>();
        metadata.stateAccessor = this.stateAccessor;
        metadata.workflowClass = workflowClazz;

        metadata.workflowProxyClass = newByteBuddy(workflowClazz)
                .subclass(WorkflowSessionBase.class)
                .implement(workflowClazz)
                .method(methodFilter(workflowClazz))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(StepKeyArgs.Binder.INSTANCE)
                        .toField("handler"))
                .make().load(workflowClazz.getClassLoader())
                .getLoaded();

        return metadata;
    }

}
