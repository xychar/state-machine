package com.xychar.stateful.engine;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface StepKeyArgs {

    /**
     * A binder for binding parameters that are annotated with {@link StepKeyArgs}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<StepKeyArgs> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Class<StepKeyArgs> getHandledType() {
            return StepKeyArgs.class;
        }

        private String buildStepKeyArgs(MethodDescription source) {
            StringBuilder stepKeyArgs = new StringBuilder();
            ParameterList<?> parameters = source.getParameters();
            for (int i = 0, count = source.getParameters().size(); i < count; i++) {
                AnnotationList declaredAnnotations = parameters.get(i).getDeclaredAnnotations();
                if (declaredAnnotations.isAnnotationPresent(StepKey.class)) {
                    stepKeyArgs.append((char) ('a' + i));
                }
            }

            return stepKeyArgs.toString();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<StepKeyArgs> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription parameterType = target.getType().asErasure();
            if (parameterType.represents(String.class)) {
                String stepKeyArgs = buildStepKeyArgs(source);
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TextConstant(stepKeyArgs));
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() + " parameter" +
                        " is annotated with a StepKeys annotation with an argument not representing a String type");
            }
        }
    }
}
