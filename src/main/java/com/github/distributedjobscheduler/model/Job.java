package com.github.distributedjobscheduler.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Data
@Getter
public class Job {

    // Immutable fields, set at creation
    private final String jobId;
    private final String jobName;
    private final Map<String, Object> payload;
    private final Instant createdAt;

    // Mutable fields, updated during the job lifecycle
    private JobStatus status;
    private Instant updatedAt;
    private int retryCount;
    private String type;
    private List<String> requiredCapabilities;

    /* TODO: To be implement in next version
   /* private final String scheduledAt;
    private final String completedAt;
    private final String errorMessage;
    private final String maxRetries;
    private final String priority;
    private final String jobType;
    private final String jobGroup;*/

    @JsonCreator
    public Job(
            @JsonProperty("jobId") String jobId,
            @JsonProperty("name") String jobName,
            @JsonProperty("type") String type,
            @JsonProperty("payload") Map<String, Object> payload,
            @JsonProperty("status") JobStatus status,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("requiredCapabilities") List<String> requiredCapabilities) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.type = type;
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
        this.status = status == null ? JobStatus.QUEUED : status;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.retryCount = retryCount;
        this.requiredCapabilities = requiredCapabilities == null ? List.of() : List.copyOf(requiredCapabilities);
    }

    // Builder factory
    public static Builder builder(String jobId, String name, String type) {
        return new Builder(jobId, name, type);
    }

    // Convenience factory for the example SubmitJobRequest (keeps example code simple)
    // Note: the example DTO lives in the example module; fully-qualified name used to avoid starter dependency.
    public static Job fromSubmitRequest(Object submitReq, String jobId) {
        // Defensive: Accept either the example DTO or a map-like structure.
        // Typical example usage: Job.fromSubmitRequest(req, jobId) in example module.
        try {
            // attempt to reflectively read fields (loose coupling to example DTO)
            Class<?> cls = submitReq.getClass();
            String name = (String) cls.getMethod("getName").invoke(submitReq);
            String type = (String) cls.getMethod("getType").invoke(submitReq);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String,Object>) cls.getMethod("getPayload").invoke(submitReq);

            return Job.builder(jobId, name, type)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .status(JobStatus.QUEUED)
                    .retryCount(0)
                    .build();
        } catch (Exception e) {
            // Fallback: create a minimal Job with jobId only
            return Job.builder(jobId, "unknown", "unknown")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .status(JobStatus.QUEUED)
                    .retryCount(0)
                    .build();
        }
    }

    // Builder
    public static final class Builder {
        private final String jobId;
        private final String name;
        private final String type;
        private Map<String, Object> payload = Map.of();
        private JobStatus status = JobStatus.QUEUED;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = createdAt;
        private int retryCount = 0;
        private List<String> requiredCapabilities = List.of();

        private Builder(String jobId, String name, String type) {
            this.jobId = Objects.requireNonNull(jobId, "jobId");
            this.name = Objects.requireNonNull(name, "name");
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload == null ? Map.of() : Map.copyOf(payload);
            return this;
        }

        public Builder status(JobStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt == null ? Instant.now() : createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder requiredCapabilities(List<String> requiredCapabilities) {
            this.requiredCapabilities = requiredCapabilities == null ? List.of() : List.copyOf(requiredCapabilities);
            return this;
        }

        public Job build() {
            return new Job(jobId, name, type, payload, status, createdAt, updatedAt, retryCount, requiredCapabilities);
        }
    }

    // equals/hashCode by jobId only (domain identity)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Job job = (Job) o;
        return Objects.equals(jobId, job.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    @Override
    public String toString() {
        return "Job{" + "jobId='" + jobId + '\'' + ", name='" + jobName + '\'' + ", type='" + type + '\'' + '}';
    }


}
