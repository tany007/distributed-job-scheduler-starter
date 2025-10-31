package com.github.distributedjobscheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "job.worker")
public class WorkerProperties {
    /**
     * Heartbeat interval in ms.
     */
    private int heartbeatInterval = 10000;

    /**
     * Worker timeout in ms to consider a worker as stale.
     */
    private int timeout = 30000;

}
