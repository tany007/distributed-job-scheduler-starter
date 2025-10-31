package com.github.distributedjobscheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class RegisterWorkerRequest {
    @Setter
    @Getter
    private String workerId;
    @Setter
    @Getter
    private String host;
    private List<String> capabilities;

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String[] capabilities) {
        this.capabilities = List.of(capabilities);
    }
}
