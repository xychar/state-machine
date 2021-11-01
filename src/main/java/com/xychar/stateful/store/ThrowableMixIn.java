package com.xychar.stateful.store;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
