package com.github.distributedjobscheduler.registry;

import com.github.distributedjobscheduler.model.Job;

import java.time.Duration;
import java.util.Optional;

public interface WorkerRegistry {

    void registerWorker(String workerId, String host, String... capabilities);
    Optional<String> findAvailableWorker(Job job);
    void updateHeartbeat(String workerId);
    void detectStaleWorkers(Duration timeout);

}
