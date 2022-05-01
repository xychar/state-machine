package com.xychar.stateful.scheduler;

import com.xychar.stateful.engine.WorkflowEngine;
import com.xychar.stateful.engine.WorkflowInstance;
import com.xychar.stateful.engine.WorkflowMetadata;
import com.xychar.stateful.engine.WorkflowState;
import com.xychar.stateful.example.BenchmarkEc2;
import com.xychar.stateful.exception.SchedulingException;
import com.xychar.stateful.store.StepStateStore;
import com.xychar.stateful.store.WorkflowRow;
import com.xychar.stateful.store.WorkflowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scan scheduled workflows and dispatch.
 */
@Component
public class WorkflowScheduler {
    /**
     * To stop scheduling, set nextRun to 100 years later
     */
    public static final long FOR_EVER = Duration.ofDays(36500).toMillis();

    /**
     * To stop scheduling, set nextRun to 200 days later
     */
    public static final long LOCKING_DELAY = Duration.ofDays(200).toMillis();

    /**
     * find threads need to run in the future 3650 days
     */
    public static final long SCAN_DURATION = Duration.ofDays(3650).toMillis();

    /**
     * find threads need to run in the future 30 seconds
     */
    public static final long SCAN_CYCLE = Duration.ofSeconds(30).toMillis();

    /**
     * find threads need to run in the future 30 seconds
     */
    public static final long SCAN_MIN_WAIT = Duration.ofMillis(500).toMillis();

    public static final int THREAD_COUNT = 5;
    public static final int BATCH_SIZE = 20;
    public static final int MAX_ROUNDS = 5;

    private final Object lock = new Object();
    private final AtomicLong reschedulingCounter = new AtomicLong(0);

    private final StepStateStore stepStateStore;
    private final WorkflowStore workflowStore;

    private ExecutorService threadPool = null;

    public WorkflowScheduler(@Autowired StepStateStore stepStateStore,
                             @Autowired WorkflowStore workflowStore) {
        this.stepStateStore = stepStateStore;
        this.workflowStore = workflowStore;
    }

    @PostConstruct
    public void initialize() {
        stepStateStore.createTableIfNotExists();
        workflowStore.createTableIfNotExists();
    }

    @PreDestroy
    public void destroy() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    public void waitForTasks(long waitingTime) throws InterruptedException {
        synchronized (lock) {
            lock.wait(waitingTime);
        }
    }

    public void rescanTasks(boolean rescan) throws InterruptedException {
        synchronized (lock) {
            if (rescan) {
                reschedulingCounter.incrementAndGet();
            }

            lock.notify();
        }
    }

    public long scanScheduledWorkflows() throws InterruptedException {
        for (int i = 0; i < MAX_ROUNDS; i++) {
            Long dueTime = Instant.now().toEpochMilli() + SCAN_DURATION;
            List<WorkflowRow> tasks = workflowStore.fetchScheduledWorkflows(dueTime, BATCH_SIZE);

            if (tasks.isEmpty()) {
                return -1;
            }

            for (WorkflowRow row : tasks) {
                long currentTime = Instant.now().toEpochMilli();
                long waitingInterval = row.nextRun - currentTime;
                if (waitingInterval <= 0) {
                    // try to lock the task by updating the next run to a far future time
                    long nextRunLocked = currentTime + LOCKING_DELAY;
                    if (workflowStore.updateWorkflowNextRun(row, null, nextRunLocked)) {
                        row.nextRun = nextRunLocked;
                        threadPool.execute(() -> {
                            try {
                                executeWorkflow(row);
                            } catch (Throwable e) {
                                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    }
                } else if (waitingInterval > SCAN_CYCLE) {
                    return SCAN_CYCLE;
                } else {
                    waitForTasks(waitingInterval);
                    long needRescheduling = reschedulingCounter.getAndSet(0);
                    if (needRescheduling > 0) {
                        break;
                    }
                }
            }
        }

        return 0L;
    }

    void executeWorkflow(WorkflowRow row) throws Throwable {
        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> workflowClazz = Class.forName(row.className, true, threadClassLoader);

        WorkflowEngine engine = new WorkflowEngine();
        engine.stateAccessor = stepStateStore;

        WorkflowMetadata<?> metadata = engine.buildFrom(workflowClazz);
        WorkflowInstance<?> session = engine.newWorkflowInstance(metadata);

        session.setExecutionId(row.sessionId);
        Object workflow = session.getWorkflowInstance();

        Method method = workflowClazz.getMethod(row.methodName);

        try {
            Object result = method.invoke(workflow);
            System.out.println("Done - step " + row.methodName + ", returned:" + result);

            long currentTime = Instant.now().toEpochMilli();
            workflowStore.updateWorkflowNextRun(row, currentTime, currentTime + FOR_EVER);
            rescanTasks(true);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SchedulingException) {
                SchedulingException se = (SchedulingException) e.getTargetException();

                long currentTime = Instant.now().toEpochMilli();
                long waitingTime = se.waitingTime >= 500 ? se.waitingTime : 500L;

                long nextRun = currentTime + waitingTime;
                workflowStore.updateWorkflowNextRun(row, currentTime, nextRun);
                rescanTasks(true);
            } else {
                throw e.getTargetException();
            }
        }
    }

    public void run() throws InterruptedException {
        threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        while (!Thread.currentThread().isInterrupted()) {
            long waitingInterval = scanScheduledWorkflows();
            if (waitingInterval < 0) {
                System.out.println("No workflows scheduled, exiting...");
                break;
            } else {
                System.out.println("No workflows scheduled within 30 seconds");
                waitForTasks(Math.max(waitingInterval, SCAN_CYCLE));
            }
        }

        threadPool.shutdownNow();
    }

    public void createWorkflowIfNotExists(String sessionId, Class<?> workflowClazz, String methodName) {
        WorkflowRow found = workflowStore.loadWorkflow(sessionId, workflowClazz.getName(), methodName, "");
        if (found != null) {
            found.nextRun = Instant.now().toEpochMilli();
            workflowStore.saveWorkflow(found);
        } else {
            WorkflowRow row = new WorkflowRow();
            row.sessionId = sessionId;
            row.className = workflowClazz.getName();
            row.methodName = methodName;
            row.stepKey = "";
            row.state = WorkflowState.Created.name();
            row.lastRun = 0L;
            row.nextRun = Instant.now().toEpochMilli();
            row.executions = 0;

            workflowStore.saveWorkflow(row);
        }
    }
}
