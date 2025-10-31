# Distributed Job Scheduler (Spring Boot Starter)

A lightweight, pluggable, HTTP-based Distributed Job Scheduler designed for JVM-based microservices. 
Built using Spring Boot, it provides real-time scheduling, job dispatching, and worker coordination — ideal for short-lived background jobs without the need for Kafka or heavyweight queuing systems.

---

## Key Features

- Spring Boot Starter — self-configuring via `application.yml`
- HTTP Push Model — real-time job dispatching to workers
- Pluggable Interfaces — bring your own storage or dispatcher
- Capability-Based Worker Matching
- Heartbeat-based Worker Registry
- Retry with Resilience4j Backoff
- Thread-Pool Configurable Scheduler Executor
- Monitoring-Ready / Actuator-Friendly

---

## Architecture Overview

| Component                 | Description                                                                                           |
|--------------------------:|------------------------------------------------------------------------------------------------------|
| **JobScheduler**          | Periodically polls `JobStorage` and dispatches pending jobs to active workers.                       |
| **JobDispatcher**         | Pushes job payloads over HTTP to worker nodes. Uses `WebClient` + Resilience4j for retries/backoff. |
| **JobStorage**            | Manages job metadata and lifecycle. Default: in-memory; can be replaced with Postgres/Mongo/etc.     |
| **WorkerRegistry**        | Tracks worker registration, capabilities, and heartbeat timestamps.                                  |
| **WorkerMonitor**         | Periodically marks workers as STALE if heartbeats are missing.                                       |
| **SchedulerTaskExecutor** | Internal thread-pool that drives the scheduler loop (configurable via properties).                   |

---

## Real-World Use Cases

- Microservice Background Jobs: Email sending, Slack/webhook triggers, PDF generation, cache refresh
- Distributed Cron Replacement: Ensures jobs executed only once in multi-node clusters
- Multi-Tenant Task Isolation: Routes jobs to tenant-specific workers with different capabilities
- Edge Worker Orchestration: Push jobs to available agents/devices over HTTP without a queue overhead
- Feature Flag-Based Execution: Sends jobs to experimental or beta-version workers using tags

---

## Core Components

| Component        | Responsibility                                               |
|------------------|---------------------------------------------------------------|
| Scheduler Engine | Fetch and dispatch jobs periodically                          |
| Job Dispatcher   | Push job via HTTP POST to worker endpoint                     |
| Job Storage      | Persist job metadata and lifecycle (Postgres, MongoDB, etc.)  |
| Worker Registry  | Tracks heartbeats and availability                            |
| Worker SDK       | Simplifies creating custom job executors                      |

---

## Configuration (`application.yml`)

```yaml
job:
  scheduler:
    enabled: true               # Enable/disable scheduler
    poll-interval-ms: 10000     # Job polling interval (10s default)
    max-retries: 3              # Max job retry attempts
    thread-pool-size: 1         # Number of threads for scheduler loop
    thread-name-prefix: djs-    # Thread name prefix
    shutdown-await-termination-ms: 5000  # Graceful shutdown timeout

  storage:
    type: in-memory             # Default: in-memory. You can provide 'postgres', 'mongo', etc.

  registry:
    type: in-memory             # Default: in-memory. Pluggable worker registry.

  worker:
    heartbeat-interval-ms: 10000
    stale-timeout-ms: 30000

```
  
### Additional Notes

- You can disable individual components via:

```yaml
job:
  dispatcher:
    enabled: false
  registry:
    enabled: false
```

- These properties are mapped to 
  - `SchedulerProperties` 
  - `WorkerProperties`

### Pluggable Architecture

The starter exposes replaceable interfaces for all major components:

| Interface        | Default Implementation              | Property Toggle               |
| ---------------- | ----------------------------------- | ----------------------------- |
| `JobStorage`     | `InMemoryJobStorage`                | `job.storage.type=in-memory`  |
| `WorkerRegistry` | `InMemoryWorkerRegistry`            | `job.registry.type=in-memory` |
| `JobDispatcher`  | `JobDispatcher` (WebClient + Retry) | `job.dispatcher.enabled=true` |


All beans use `@ConditionalOnMissingBean` or `@ConditionalOnProperty`, allowing you to provide your own Spring beans or disable defaults.

### Thread Pool Configuration

The scheduler runs using an internal `ScheduledExecutorService` managed by `SchedulerTaskExecutor`.

```yaml
job:
  scheduler:
    thread-pool-size: 2
    thread-name-prefix: djs-scheduler-
    shutdown-await-termination-ms: 10000
```

This controls concurrency and graceful shutdown behavior.

## Auto-Configuration Mechanism
Spring Boot automatically loads the scheduler via:

```cmd
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
Containing:
```cmd
com.github.distributedjobscheduler.config.SchedulerAutoConfiguration
```

At runtime:

- Spring Boot reads this file.
- Instantiates SchedulerAutoConfiguration.
- Applies conditional annotations (@ConditionalOnMissingBean, @ConditionalOnProperty).
- Creates and wires beans such as:
  - JobScheduler
  - SchedulerTaskExecutor
  - WorkerRegistry
  - JobDispatcher
- Binds user configuration from application.yml into SchedulerProperties and WorkerProperties.

** No manual configuration is needed — it activates automatically when on the classpath.

## Excluding Controllers (Design Decision)

This starter is a pure backend library — it does not ship any REST controllers like `JobController` or `WorkerController`. 
Application authors are free to expose their own REST API or UI layer. This avoids forcing specific API contracts or endpoint mappings on consuming applications.

### Packaging & POM Setup
** For this Starter (library) **
Since this is a Spring Boot Starter, you should not inherit the Boot parent POM.

```xml
<properties>
  <java.version>17</java.version>
  <spring.boot.version>3.2.5</spring.boot.version>
</properties>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <version>${spring.boot.version}</version>
  </dependency>

  <dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <version>${spring.boot.version}</version>
    <optional>true</optional>
  </dependency>
</dependencies>
```
**Why no** <parent>?
Starters should stay version-neutral so that consuming applications can use any compatible Spring Boot version.

## How to Override / Extend Components
You can easily plug in your own beans:

### Example: Custom Storage

```java
@Configuration
public class CustomJobStorageConfig {
    @Bean
    public JobStorage postgresJobStorage(DataSource ds) {
        return new PostgresJobStorage(ds);
    }
}
```

### Example: Custom Dispatcher

```java
@Bean
public JobDispatcher kafkaDispatcher(KafkaTemplate<String, String> template) {
    return new KafkaJobDispatcher(template);
}
```
Spring Boot will automatically skip creating the default in-memory or HTTP versions.


## Best Practices

- Always enable `@EnableScheduling` only if you run your own monitors; the starter’s internal executor doesn’t require it.
- Override `SchedulerTaskExecutor` or `JobStorage` only if necessary.
- Secure all custom APIs if you expose job management endpoints.
- Keep worker heartbeat intervals consistent with stale detection timeouts.

## Author

- Tanmoy Khan\- [`tany007`](https://github.com/tany007)
