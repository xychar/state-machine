package com.xychar.stateful.engine;

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

import java.time.Instant;
import java.util.List;

/**
 * Scan scheduled executions and dispatch.
 */
public class WorkflowScheduler {

    List<WorkflowThreadData> scanScheduledThreads(Instant dueTime) {
        // nextRun <= dueTime and nextRun > lastRun
        // find threads need to run in the future 30 seconds
        // then wait for the period before the nearest thread
        return null;
    }

}
