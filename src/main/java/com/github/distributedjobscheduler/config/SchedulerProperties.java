package com.github.distributedjobscheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "job.scheduler")
public class SchedulerProperties {

    /**
     * Whether the scheduler should auto-start. Default true.
     */
    private boolean enabled = true;

    /**
     * Poll interval in milliseconds (default 10s).
     */
    private long pollIntervalMs = 10_000L;

    /**
     * Max retries for a job (default 3).
     */
    private int maxRetries = 3;

    /**
     * Number of threads in the scheduler executor (default 1).
     */
    private int threadPoolSize = 1;

    /**
     * Thread name prefix for threads created by SchedulerTaskExecutor.
     */
    private String threadNamePrefix = "distributed-job-scheduler-";

    /**
     * How long to wait (ms) for the executor to terminate during shutdown.
     */
    private long shutdownAwaitTerminationMs = 5_000L;

    // getters and setters

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }

    public void setThreadNamePrefix(String threadNamePrefix) { this.threadNamePrefix = threadNamePrefix; }

    public void setShutdownAwaitTerminationMs(long shutdownAwaitTerminationMs) { this.shutdownAwaitTerminationMs = shutdownAwaitTerminationMs; }
}
