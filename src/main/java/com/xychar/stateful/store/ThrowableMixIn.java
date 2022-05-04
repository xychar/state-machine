package com.xychar.stateful.store;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Exclude the stacktrace when serializing exception.
 */
public interface ThrowableMixIn {
    @JsonIgnore
    Throwable getCause();

    @JsonIgnore
    StackTraceElement[] getStackTrace();

    @JsonIgnore
    String getLocalizedMessage();

    @JsonIgnore
    Throwable[] getSuppressed();
}
