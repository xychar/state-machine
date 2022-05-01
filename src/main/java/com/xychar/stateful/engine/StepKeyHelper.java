package com.xychar.stateful.engine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class StepKeyHelper {
    private static boolean isAnnotationPresent(Annotation[] annotations, Class<? extends Annotation> annotationType) {
        for (Annotation ann : annotations) {
            if (ann.annotationType().equals(annotationType)) {
                return true;
            }
        }

        return false;
    }

    public static char argIndexToChar(int value) {
        if (value >= 0 && value < 10) {
            return (char) ('0' + value);
        } else if (value < 36) {
            return (char) ('a' + (value - 10));
        } else {
            throw new IndexOutOfBoundsException("Too many arguments");
        }
    }

    public static String buildStepKeyArgs(Method stepMethod) {
        StringBuilder stepKeyArgs = new StringBuilder();
        Annotation[][] annotations = stepMethod.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (isAnnotationPresent(annotations[i], StepKey.class)) {
                stepKeyArgs.append(argIndexToChar(i));
            }
        }

        return stepKeyArgs.toString();
    }

    public static int charToArgIndex(char value) {
        if (value >= '0' && value <= '9') {
            return (int) (value - '0');
        } else if (value >= 'a' && value <= 'z') {
            return (int) (value - 'a') + 10;
        } else {
            throw new IndexOutOfBoundsException("Invalid argument index");
        }
    }

    public static Object[] getStepKeys(String stepKeyArgs, Object[] args) {
        if (stepKeyArgs != null && !stepKeyArgs.isEmpty()) {
            Object[] stepKeys = new Object[stepKeyArgs.length()];
            for (int i = 0; i < stepKeyArgs.length(); i++) {
                int argIndex = charToArgIndex(stepKeyArgs.charAt(i));
                stepKeys[i] = args[argIndex];
            }

            return stepKeys;
        } else {
            return new Object[0];
        }
    }
}
