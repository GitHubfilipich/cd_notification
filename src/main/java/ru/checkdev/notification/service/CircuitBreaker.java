package ru.checkdev.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@Slf4j
public class CircuitBreaker {
    private final int failureThreshold = 3;
    private int failureCount = 0;
    private final int maxOpenPeriod = 10000;
    private LocalDateTime startOpenPeriod;
    private State state = State.CLOSED;

    private enum State {
        CLOSED,
        OPEN
    }

    public <R> R exec(Supplier<R> execution, Consumer<Exception> exceptionExecution) {
        if (state == State.OPEN && startOpenPeriod != null) {
            var duration = java.time.Duration.between(startOpenPeriod, LocalDateTime.now()).toMillis();
            if (duration >= maxOpenPeriod) {
                state = State.CLOSED;
                failureCount = 0;
                log.info("Circuit Breaker CLOSED after open period.");
            }
        }
        if (state == State.CLOSED) {
            try {
                return execution.get();
            } catch (Exception e) {
                failureCount++;
                log.error("Attempt failed, failure count: {}", failureCount);
                if (failureCount >= failureThreshold) {
                    state = State.OPEN;
                    startOpenPeriod = LocalDateTime.now();
                    log.warn("Circuit Breaker OPENED due to failure threshold exceeded.");
                }
                exceptionExecution.accept(e);
            }
        }
        log.warn("Circuit Breaker is OPEN. Skipping request.");
        throw new CircuitBreakerOpenException("Circuit Breaker is OPEN. Request skipped.");
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
