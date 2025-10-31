package com.github.distributedjobscheduler.registry.implementation;

import com.github.distributedjobscheduler.model.Job;
import com.github.distributedjobscheduler.model.Worker;
import com.github.distributedjobscheduler.model.WorkerStatus;
import com.github.distributedjobscheduler.registry.WorkerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryWorkerRegistry implements WorkerRegistry {

    private final Map<String, Worker> workers = new ConcurrentHashMap<>();

    @Override
    public void registerWorker(String workerId, String host, String... capabilities) {
        Worker worker = new Worker();
        worker.setWorkerId(workerId);
        worker.setHost(host);
        worker.setCapabilities(Arrays.asList(capabilities));
        worker.setLastHeartbeat(Instant.now());
        worker.setStatus(WorkerStatus.ACTIVE);
        workers.put(workerId, worker);

        log.info("Worker registered: id={}, host={}, capabilities={}", workerId, host, Arrays.toString(capabilities));
    }

    @Override
    public Optional<String> findAvailableWorker(Job job) {
        String requiredCapability = job.getType(); // Assuming Job has a "type" field
        return workers.values().stream()
                .filter(worker -> worker.getStatus() == WorkerStatus.ACTIVE)
                .filter(worker -> worker.getCapabilities().contains(requiredCapability))
                .findAny()
                .map(Worker::getHost);
    }

    @Override
    public void updateHeartbeat(String workerId) {
        Worker worker = workers.get(workerId);
        if (worker != null) {
            worker.setLastHeartbeat(Instant.now());
            if (worker.getStatus() != WorkerStatus.ACTIVE) {
                worker.setStatus(WorkerStatus.ACTIVE);
                log.info("Worker {} marked ACTIVE via heartbeat", workerId);
            }
        } else {
            log.warn("Heartbeat received from unknown worker: id={}", workerId);
        }
    }

    @Override
    public void detectStaleWorkers(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        for (Worker worker : workers.values()) {
            if (worker.getLastHeartbeat().isBefore(cutoff)) {
                if (worker.getStatus() != WorkerStatus.STALE) {
                    worker.setStatus(WorkerStatus.STALE);
                    log.warn("Worker {} marked STALE (last seen at {})", worker.getWorkerId(), worker.getLastHeartbeat());
                }
            }
        }
    }

    public List<Worker> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }

    public void setWorkerStatus(String workerId, WorkerStatus status) {
        Worker worker = workers.get(workerId);
        if (worker != null) {
            worker.setStatus(status);
            log.info("Worker status updated: id={}, status={}", workerId, status);
        }
    }
}
