package com.github.distributedjobscheduler.storage;

import com.github.distributedjobscheduler.model.Job;
import com.github.distributedjobscheduler.model.JobStatus;

import java.util.List;

public interface JobStorage {

    void save(Job job);
    void updateStatus(String jobId, JobStatus status);
    Job findById(String jobId);
    List<Job> findAll();
    List<Job> getPendingJobs();
}
