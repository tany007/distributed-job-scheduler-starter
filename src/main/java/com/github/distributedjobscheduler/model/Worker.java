package com.github.distributedjobscheduler.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class Worker {

    private String workerId;
    private String host;
    private List<String> capabilities;
    private Instant lastHeartbeat;
    private WorkerStatus status;

}
