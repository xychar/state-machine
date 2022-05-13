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
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    public Map<String, Integer> getMethodNames(Class<?> clazz) {
        Map<String, Integer> methods = new LinkedHashMap<>();
        for (Method m : clazz.getMethods()) {
            methods.putIfAbsent(m.getName(), methods.size());
        }

        return methods;
    }

    public void getAllInterfaces(Set<String> names, Class<?> clazz) {
        names.add(clazz.getName());
        for (Class<?> parent : clazz.getInterfaces()) {
            getAllInterfaces(names, parent);
        }
    }

    public <T> ContainerMetadata<T> buildFrom(Class<T> containerClazz) {
        ClassLoader classLoader = containerClazz.getClassLoader();

        Map<String, Integer> methodNames = MethodIndex.Binder.INSTANCE.getMethodNames(containerClazz);
        if (methodNames == null) {
            methodNames = getMethodNames(containerClazz);

            Set<String> interfaceNames = new LinkedHashSet<>();
            getAllInterfaces(interfaceNames, containerClazz);
            for (String className : interfaceNames) {
                MethodIndex.Binder.INSTANCE.addMethodNames(className, methodNames);
            }
        }

        ContainerMetadata<T> metadata = new ContainerMetadata<>();
        metadata.methodNames = methodNames;

        DynamicType.Unloaded<ContainerProxy> dynamicType = newByteBuddy(containerClazz)
                .subclass(ContainerProxy.class)
                .implement(containerClazz)
                .defineField("hello", String.class, Modifier.PRIVATE)
                .defineField("world", String.class, Modifier.PRIVATE)
                .method(methodFilter(containerClazz))
                .intercept(MethodDelegation.withEmptyConfiguration()
                        .withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS)
                        .withBinders(MethodIndex.Binder.INSTANCE)
                        .withResolvers(
                                MethodNameEqualityResolver.INSTANCE,
                                ParameterLengthResolver.INSTANCE,
                                ArgumentTypeResolver.INSTANCE,
                                DeclaringTypeResolver.INSTANCE,
                                BindingPriority.Resolver.INSTANCE)
                        .toField("handler"))
                .make();

        metadata.containerClass = containerClazz;
        metadata.containerProxyClass = dynamicType.load(classLoader).getLoaded();

        try {
            metadata.containerConstructor = metadata.containerProxyClass.getConstructor();
            return metadata;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowException("Failed to locate the default constructor", e);
        }
    }

}
