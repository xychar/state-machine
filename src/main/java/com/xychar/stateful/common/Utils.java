package com.xychar.stateful.common;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class Utils {

    public static <T, R> R callIfNotNull(T obj, Function<T, R> func) {
        return obj != null ? func.apply(obj) : null;
    }

    public static <R> R callIfNotBlank(String s, Function<String, R> func) {
        return StringUtils.isNotBlank(s) ? func.apply(s) : null;
    }

}
