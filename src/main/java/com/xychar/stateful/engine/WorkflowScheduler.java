package com.xychar.stateful.engine;

import java.time.Instant;
import java.util.List;

/**
 * Scan scheduled executions and dispatch.
 */
public class WorkflowScheduler {

    List<WorkflowThread> scanScheduledThreads(Instant dueTime) {
        // nextRun <= dueTime and nextRun > lastRun
        // find threads need to run in the future 30 seconds
        // then wait for the period before the nearest thread
        return null;
    }

}
