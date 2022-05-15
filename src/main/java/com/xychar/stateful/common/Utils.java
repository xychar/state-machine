package com.xychar.stateful.common;

import com.xychar.stateful.engine.Startup;
import com.xychar.stateful.exception.StepNotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    static final Pattern VAR_TAG = Pattern.compile("(\\\\$)|(\\$\\{\\s*(\\w\\S+).*?\\})");

    public static String format(String template, Properties params) {
        StringBuffer sb = new StringBuffer();
        Matcher m = VAR_TAG.matcher(template);
        while (m.find()) {
            if (m.group(1) != null) {
                m.appendReplacement(sb, "$");
            } else if (m.group(3) != null) {
                String value = params.getProperty(m.group(3), "");
                m.appendReplacement(sb, value);
            }
        }

        m.appendTail(sb);
        return sb.toString();
    }

    public static <T, R> R callIfNotNull(T obj, Function<T, R> func) {
        return obj != null ? func.apply(obj) : null;
    }

    public static <R> R callIfNotBlank(String s, Function<String, R> func) {
        return StringUtils.isNotBlank(s) ? func.apply(s) : null;
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Locate the workflow entry method.
     */
    public static Method getStepMethod(Class<?> workflowClazz, String methodName) {
        if (methodName != null && !methodName.isEmpty()) {
            try {
                // The step method should have no parameters
                return workflowClazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new StepNotFoundException("Step not found: " + methodName, e);
            }
        } else {
            // Search first startup step in the declared methods of the workflow class
            for (Method m : workflowClazz.getDeclaredMethods()) {
                if (m.getAnnotation(Startup.class) != null) {
                    return m;
                }
            }

            throw new StepNotFoundException("Default step not found");
        }
    }
}
