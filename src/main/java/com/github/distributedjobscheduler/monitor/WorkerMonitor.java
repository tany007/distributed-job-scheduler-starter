package com.github.distributedjobscheduler.monitor;

import com.github.distributedjobscheduler.config.WorkerProperties;
import com.github.distributedjobscheduler.registry.implementation.InMemoryWorkerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkerMonitor {

    private static final Logger log = LoggerFactory.getLogger(WorkerMonitor.class);

    private final InMemoryWorkerRegistry inMemoryWorkerRegistry;
    private final WorkerProperties workerProperties;

    public WorkerMonitor(InMemoryWorkerRegistry inMemoryWorkerRegistry, WorkerProperties workerProperties) {
        this.inMemoryWorkerRegistry = inMemoryWorkerRegistry;
        this.workerProperties = workerProperties;
    }

    /**
     * Runs periodically to detect stale workers.
     * Interval controlled by job.worker.heartbeat-interval-ms property.
     */
    @Scheduled(fixedDelayString = "${job.worker.heartbeat-interval-ms:30000}")
    public void detectStaleWorkers() {
        log.debug("WorkerMonitor: starting stale-worker detection (timeout={}ms)", workerProperties.getTimeout());
        inMemoryWorkerRegistry.detectStaleWorkers(Duration.ofMillis(workerProperties.getTimeout()));
        log.debug("WorkerMonitor: completed stale-worker detection");
    }
}
