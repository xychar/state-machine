package com.xychar.stateful.container;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MethodIndex {

    /**
     * A binder for binding parameters that are annotated with {@link MethodIndex}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<MethodIndex> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        private final Map<String, Map<String, Integer>> methodIndexes;

        private Binder() {
            methodIndexes = new LinkedHashMap<>();
        }

        /**
         * {@inheritDoc}
         */
        public Class<MethodIndex> getHandledType() {
            return MethodIndex.class;
        }

        public void addMethodNames(String className, Map<String, Integer> methods) {
            methodIndexes.put(className, methods);
        }

        public Map<String, Integer> getMethodNames(Class<?> clazz) {
            return methodIndexes.get(clazz.getName());
        }

        private StackManipulation getMethodIndex(MethodDescription source) {
            Map<String, Integer> methodNames = methodIndexes.get(source.getDeclaringType().getTypeName());
            if (methodNames != null) {
                Integer methodIndex = methodNames.get(source.getName());
                if (methodIndex != null) {
                    return IntegerConstant.forValue(methodIndex);
                }
            }

            return IntegerConstant.MINUS_ONE;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<MethodIndex> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription parameterType = target.getType().asErasure();
            if (parameterType.represents(Integer.class) || parameterType.represents(Integer.TYPE)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(getMethodIndex(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() + " parameter" +
                        " is annotated with a MethodKind annotation with an argument not representing a Integer type");
            }
        }
    }
}
