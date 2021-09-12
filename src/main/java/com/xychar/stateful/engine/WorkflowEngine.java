package com.xychar.stateful.engine;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class WorkflowEngine {

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
     * Only intercept methods
     */
    private ElementMatcher<ByteCodeElement> methodFilter(Class<?> clazz) {
        return ElementMatchers.isDeclaredBy(
                ElementMatchers.isSuperTypeOf(clazz)
                        .and(ElementMatchers.isInterface())
        );
    }

    public <T> WorkflowMetadata<T> buildFrom(Class<T> workflowClazz) {
        WorkflowMetadata<T> metadata = new WorkflowMetadata<T>();
        metadata.workflowClass = workflowClazz;

        ByteBuddy buddy = newByteBuddy(workflowClazz);

        metadata.workflowProxyClass = buddy
                .subclass(WorkflowSessionBase.class)
                .implement(workflowClazz)
                .method(methodFilter(workflowClazz))
                // .intercept(InvocationHandlerAdapter.toField("handler"))
                .intercept(MethodDelegation.to(StepInterceptor.class))
                .make()
                .load(workflowClazz.getClassLoader()) //, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

        return metadata;
    }

}
