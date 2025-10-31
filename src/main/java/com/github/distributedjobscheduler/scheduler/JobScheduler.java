package com.github.distributedjobscheduler.scheduler;

import com.github.distributedjobscheduler.config.SchedulerProperties;
import com.github.distributedjobscheduler.dispatcher.JobDispatcher;
import com.github.distributedjobscheduler.model.Job;
import com.github.distributedjobscheduler.model.JobStatus;
import com.github.distributedjobscheduler.registry.WorkerRegistry;
import com.github.distributedjobscheduler.storage.JobStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * Periodically fetch jobs from storage (e.g., with status QUEUED or RETRY).
 * Select an appropriate available worker for each job.
 * Dispatch the job to the selected worker (via HTTP).
 * Handle success/failure and retry logic.
 * Respect configuration like poll interval and retry limits.
 * */

public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobStorage jobStorage;
    private final WorkerRegistry workerRegistry;
    private final JobDispatcher jobDispatcher;
    private final SchedulerTaskExecutor executor;
    private final SchedulerProperties props;

    // handle for scheduled task so we can cancel on stop
    private ScheduledFuture<?> scheduledFuture;

    public JobScheduler(JobStorage jobStorage,
                        WorkerRegistry workerRegistry,
                        JobDispatcher jobDispatcher,
                        SchedulerTaskExecutor executor,
                        SchedulerProperties props) {
        this.jobStorage = jobStorage;
        this.workerRegistry = workerRegistry;
        this.jobDispatcher = jobDispatcher;
        this.executor = executor;
        this.props = props;
    }

    /**
     * Starts the scheduler (schedules periodic execution of pollAndDispatch()).
     * Safe to call multiple times — will not schedule duplicates.
     */
    public synchronized void start() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            log.warn("JobScheduler already started");
            return;
        }
        long interval = props.getPollIntervalMs();
        log.info("Starting JobScheduler (pollIntervalMs={}, maxRetries={})", interval, props.getMaxRetries());
        scheduledFuture = executor.scheduleAtFixedRate(this::pollAndDispatch, 0, interval);
    }

    /**
     * Stops the scheduler and cancels the scheduled task.
     */
    public synchronized void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
            log.info("JobScheduler stopped");
        } else {
            log.debug("JobScheduler stop() called but scheduler was not running");
        }
    }

    /**
     * Main polling / dispatch logic. This replaces the old run() method.
     * Kept public for easier unit testing.
     */
    public void pollAndDispatch() {
        try {
            List<Job> pending = jobStorage.getPendingJobs();
            if (pending.isEmpty()) {
                log.debug("No pending jobs found");
                return;
            }

            for (Job job : pending) {
                try {
                    Optional<String> workerUrl = workerRegistry.findAvailableWorker(job);
                    if (workerUrl.isEmpty()) {
                        log.debug("No available worker for jobId={}", job.getJobId());
                        continue;
                    }

                    boolean success = jobDispatcher.dispatch(job, workerUrl.get());
                    if (success) {
                        jobStorage.updateStatus(job.getJobId(), JobStatus.IN_PROGRESS);
                        log.info("Dispatched jobId={} to worker={}", job.getJobId(), workerUrl.get());
                    } else {
                        handleRetry(job);
                    }
                } catch (Exception inner) {
                    log.error("Error dispatching job {}: {}", job.getJobId(), inner.getMessage(), inner);
                    handleRetry(job);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in pollAndDispatch: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry bookkeeping — increments retry count and persists status or fails permanently.
     */
    private void handleRetry(Job job) {
        int maxRetries = props.getMaxRetries();
        int current = job.getRetryCount();
        if (current >= maxRetries) {
            jobStorage.updateStatus(job.getJobId(), JobStatus.FAILED);
            log.warn("Job {} exceeded max retries ({}). Marking FAILED.", job.getJobId(), maxRetries);
        } else {
            job.setRetryCount(current + 1);
            job.setStatus(JobStatus.RETRY);
            jobStorage.save(job); // save() should upsert
            log.info("Job {} scheduled for retry (attempt={} of {})", job.getJobId(), job.getRetryCount(), maxRetries);
        }
    }
}