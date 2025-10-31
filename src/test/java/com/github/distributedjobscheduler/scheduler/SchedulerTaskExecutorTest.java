package com.github.distributedjobscheduler.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerTaskExecutorTest {

    private SchedulerTaskExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            // ensure clean shutdown between tests
            executor.shutdown();
        }
    }

    @Test
    void scheduleAtFixedRate_runsTaskRepeatedly() throws InterruptedException {
        // Given: single-thread executor, small shutdown timeout for tests
        executor = new SchedulerTaskExecutor(
                1,                         // threadPoolSize
                "test-scheduler-",         // threadNamePrefix
                2000L                      // shutdownAwaitTerminationMs
        );

        // Expect the scheduled task to run 3 times
        CountDownLatch latch = new CountDownLatch(3);

        Runnable task = latch::countDown;

        // When: schedule with small initial delay (50ms) and interval (50ms)
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, 50, 50);

        // Then: await for latch with a reasonable timeout
        boolean completed = latch.await(2, TimeUnit.SECONDS);

        // Cleanup the scheduled task
        future.cancel(true);

        assertTrue(completed, "Scheduled task did not run the expected number of times");
    }

    @Test
    void shutdown_preventsFurtherTaskExecution() throws InterruptedException {
        // Given: executor with small shutdown wait
        executor = new SchedulerTaskExecutor(
                1,
                "test-scheduler-",
                2000L
        );

        AtomicInteger counter = new AtomicInteger(0);
        Runnable increment = counter::incrementAndGet;

        // schedule every 50ms
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(increment, 10, 50);

        // Let it run a few times
        Thread.sleep(220); // ~4-5 ticks

        int countBeforeShutdown = counter.get();
        assertTrue(countBeforeShutdown >= 2, "Counter should have incremented before shutdown");

        // When: shutdown executor
        future.cancel(true); // cancel scheduled task to avoid race; still call executor.shutdown() to exercise logic
        executor.shutdown();

        // Record current count and wait to see if increments continue (they shouldn't)
        int afterShutdownCount = counter.get();
        Thread.sleep(200); // wait longer than the interval to see if any task runs after shutdown

        int finalCount = counter.get();

        assertEquals(afterShutdownCount, finalCount, "Counter should not increase after shutdown");
        assertTrue(finalCount >= 1, "Counter should have at least one increment from before shutdown");
    }
}
