package com.github.distributedjobscheduler.scheduler;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SchedulerTaskExecutor {

    private final ScheduledExecutorService executorService;

    private static final Logger log = LoggerFactory.getLogger(SchedulerTaskExecutor.class);

    // configured via SchedulerProperties
    private final long shutdownAwaitTerminationMs;

    private final String threadNamePrefix;

    public SchedulerTaskExecutor(int threadPoolSize, String threadNamePrefix, long shutdownAwaitTerminationMs) {
        this.shutdownAwaitTerminationMs = shutdownAwaitTerminationMs;
        this.threadNamePrefix = threadNamePrefix;

        if (threadPoolSize <= 1) {
            this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setName(threadNamePrefix + "0");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.executorService = Executors.newScheduledThreadPool(threadPoolSize, r -> {
                Thread t = new Thread(r);
                t.setName(threadNamePrefix + UUIDThreadSuffix.nextSuffix());
                t.setDaemon(true);
                return t;
            });
        }
    }


    /**
     * Starts a scheduled task with the given interval.
     *
     * @param task            The task to execute.
     * @param initialDelayMs  Initial delay before execution.
     * @param intervalMs      Interval between executions.
     * @return ScheduledFuture for advanced control (e.g., cancel).
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelayMs, long intervalMs) {
        return executorService.scheduleAtFixedRate(task, initialDelayMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SchedulerTaskExecutor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("SchedulerTaskExecutor did not terminate in time; invoking shutdownNow()");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("SchedulerTaskExecutor did not terminate after shutdownNow()");
                }
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while shutting down SchedulerTaskExecutor; invoking shutdownNow()", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SchedulerTaskExecutor shutdown complete.");
    }

    private static final class UUIDThreadSuffix {
        private static final AtomicInteger COUNTER = new AtomicInteger(0);
        static String nextSuffix() {
            return String.valueOf(COUNTER.getAndIncrement());
        }
    }
}

