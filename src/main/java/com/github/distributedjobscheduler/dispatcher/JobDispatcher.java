package com.github.distributedjobscheduler.dispatcher;

import com.github.distributedjobscheduler.model.Job;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.function.Supplier;

/*
 HTTP push with retry/backoff
 */

public class JobDispatcher {

    private final WebClient webClient;

    private final Retry retry;

    public JobDispatcher(WebClient.Builder webClientBuilder, @Qualifier("jobDispatchRetry")Retry retry) {
        this.webClient = webClientBuilder
                .build();
        this.retry = retry;
    }

    /**
     * Dispatches a job synchronously using retry logic.
     *
     * @param job       The job to send.
     * @param workerUrl The full worker URL (e.g., http://worker1.local:8080).
     * @return true if the job was delivered with 2xx response, false otherwise.
     */
    public boolean dispatch(Job job, String workerUrl) {
        Supplier<Boolean> dispatchSupplier = Retry.decorateSupplier(retry, () -> {
            HttpStatusCode status = webClient.post()
                    .uri(workerUrl + "/execute-job")
                    .bodyValue(job)
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode)
                    .block(Duration.ofSeconds(5)); // synchronous call

            return status != null && status.is2xxSuccessful();
        });

        try {
            return dispatchSupplier.get();
        } catch (Exception e) {
            System.err.println("Job dispatch failed after retries: " + e.getMessage());
            return false;
        }
    }
}
