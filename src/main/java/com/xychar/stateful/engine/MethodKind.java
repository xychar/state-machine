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
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MethodKind {
    int METHOD_KIND_NONE = 0;
    int METHOD_KIND_STEP = 1;
    int METHOD_KIND_SUB_STEP = 2;
    int METHOD_KIND_INPUT = 3;
    int METHOD_KIND_OUTPUT = 4;
    int METHOD_KIND_INJECT = 5;

    IntegerConstant STEP = IntegerConstant.ONE;
    IntegerConstant SUB_STEP = IntegerConstant.TWO;
    IntegerConstant INPUT = IntegerConstant.THREE;
    IntegerConstant OUTPUT = IntegerConstant.FOUR;
    IntegerConstant INJECT = IntegerConstant.FIVE;

    /**
     * A binder for binding parameters that are annotated with {@link MethodKind}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<MethodKind> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Class<MethodKind> getHandledType() {
            return MethodKind.class;
        }

        private IntegerConstant getMethodKind(MethodDescription source) {
            AnnotationList annotations = source.getDeclaredAnnotations();
            if (annotations.isAnnotationPresent(Step.class)) {
                return STEP;
            } else if (annotations.isAnnotationPresent(Main.class)) {
                return STEP;
            } else if (annotations.isAnnotationPresent(SubStep.class)) {
                return SUB_STEP;
            } else if (annotations.isAnnotationPresent(Input.class)) {
                return INPUT;
            } else if (annotations.isAnnotationPresent(Output.class)) {
                return OUTPUT;
            } else if (annotations.isAnnotationPresent(Inject.class)) {
                return INJECT;
            }

            return IntegerConstant.ZERO;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<MethodKind> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription parameterType = target.getType().asErasure();
            if (parameterType.represents(Integer.class) || parameterType.represents(Integer.TYPE)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(getMethodKind(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() + " parameter" +
                        " is annotated with a MethodKind annotation with an argument not representing a Integer type");
            }
        }
    }
}
