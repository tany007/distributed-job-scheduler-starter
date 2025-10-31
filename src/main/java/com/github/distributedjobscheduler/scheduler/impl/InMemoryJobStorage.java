package com.github.distributedjobscheduler.scheduler.impl;

import com.github.distributedjobscheduler.model.Job;
import com.github.distributedjobscheduler.model.JobStatus;
import com.github.distributedjobscheduler.storage.JobStorage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJobStorage implements JobStorage {
    private final Map<String, Job> jobMap = new ConcurrentHashMap<>();

    @Override
    public void save(Job job) {
        jobMap.put(job.getJobId(), job);
    }

    @Override
    public void updateStatus(String jobId, JobStatus status) {
        Job job = jobMap.get(jobId);
        if (job != null) {
            job.setStatus(status);
            job.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public Job findById(String jobId) {
        return jobMap.get(jobId);
    }

    @Override
    public List<Job> findAll() {
        return List.copyOf(jobMap.values());
    }

    @Override
    public List<Job> getPendingJobs() {
        return jobMap.values().stream()
                .filter(job -> job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RETRY)
                .toList();
    }
}
