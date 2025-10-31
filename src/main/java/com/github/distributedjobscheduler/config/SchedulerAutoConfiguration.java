package com.github.distributedjobscheduler.config;

import com.github.distributedjobscheduler.dispatcher.JobDispatcher;
import com.github.distributedjobscheduler.registry.WorkerRegistry;
import com.github.distributedjobscheduler.registry.implementation.InMemoryWorkerRegistry;
import com.github.distributedjobscheduler.scheduler.JobScheduler;
import com.github.distributedjobscheduler.scheduler.SchedulerTaskExecutor;
import com.github.distributedjobscheduler.scheduler.impl.InMemoryJobStorage;
import com.github.distributedjobscheduler.storage.JobStorage;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Autoconfiguration for the Distributed Job Scheduler starter.
 * Exposes beans with sensible defaults, but allows the application author
 * to override any bean by defining their own.
 */

@Configuration
@EnableConfigurationProperties({SchedulerProperties.class, WorkerProperties.class})
public class SchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "job.storage", name = "type", havingValue = "in-memory", matchIfMissing = true) // Default to in-memory
    public JobStorage inMemoryJobStorage() {
        return new InMemoryJobStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "job.registry", name = "type", havingValue = "in-memory", matchIfMissing = true) // Default to in-memory
    public WorkerRegistry inMemoryWorkerRegistry() {
        return new InMemoryWorkerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public SchedulerTaskExecutor schedulerTaskExecutor(SchedulerProperties props) {
        int poolSize = Math.max(1, props.getThreadPoolSize());
        String prefix = props.getThreadNamePrefix() != null ? props.getThreadNamePrefix() : "distributed-job-scheduler-";
        long shutdownMs = props.getShutdownAwaitTerminationMs() > 0 ? props.getShutdownAwaitTerminationMs() : 5000L;
        return new SchedulerTaskExecutor(poolSize, prefix, shutdownMs);
    }

    @Bean
    @ConditionalOnMissingBean(name = "jobDispatchRetry")
    public Retry jobDispatchRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("job-dispatch-retry");
    }

    @Bean
    @ConditionalOnMissingBean
    public JobDispatcher jobDispatcher(WebClient.Builder webClientBuilder,
                                       @Qualifier("jobDispatchRetry") Retry retry) {
        return new JobDispatcher(webClientBuilder, retry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "job.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true) // Enabled by default
    public JobScheduler jobScheduler(JobStorage jobStorage,
                                     WorkerRegistry workerRegistry,
                                     JobDispatcher jobDispatcher,
                                     SchedulerTaskExecutor executor,
                                     SchedulerProperties schedulerProperties) {
        JobScheduler scheduler = new JobScheduler(
                jobStorage,
                workerRegistry,
                jobDispatcher,
                executor,
                schedulerProperties
        );

        // Auto-start the scheduler if enabled (default true)
        if (schedulerProperties.isEnabled()) {
            scheduler.start();
        }
        return scheduler;
    }
}
