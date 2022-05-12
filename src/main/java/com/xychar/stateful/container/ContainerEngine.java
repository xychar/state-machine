package com.xychar.stateful.container;

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
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class ContainerEngine {

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

    public interface FieldGetter {
        Object getValue();
    }

    public interface FieldSetter {
        void setValue(Object value);
    }

    public Constructor<? extends ContainerProxy> buildContainerProxy(Class<?> containerClazz) {
        ClassLoader classLoader = containerClazz.getClassLoader();

        DynamicType.Unloaded<ContainerProxy> dynamicType = newByteBuddy(containerClazz)
                .subclass(ContainerProxy.class)
                .implement(containerClazz)
                .defineField("hello", String.class, Modifier.PRIVATE)
                .defineField("world", String.class, Modifier.PRIVATE)
                .method(methodFilter(containerClazz))
                .intercept(MethodDelegation.withEmptyConfiguration()
                        .withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS)
                        .withBinders(FieldProxy.Binder.install(GetterAndSetter.class))
                        .withResolvers(
                                MethodNameEqualityResolver.INSTANCE,
                                ParameterLengthResolver.INSTANCE,
                                ArgumentTypeResolver.INSTANCE,
                                DeclaringTypeResolver.INSTANCE,
                                BindingPriority.Resolver.INSTANCE)
                        .toField("handler"))
                .make();

        Class<? extends ContainerProxy> proxyClass = dynamicType.load(classLoader).getLoaded();

        try {
            return proxyClass.getConstructor();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to locate the default constructor", e);
        }
    }
}
