package com.github.yewyc.benchmark;

import com.github.yewyc.loadgenerator.LoadStrategy;

import java.time.Duration;

/**
 * Immutable record representing benchmark configuration parameters.
 * 
 * @param threads Number of threads to use for the benchmark (must be > 0)
 * @param duration Duration of the benchmark test phase
 * @param rate Operations per second. Ignored if it is a closed model.
 * @param connections Number of concurrent connections to maintain
 * @param urlBase Base URL to benchmark against
 * @param warmUpDuration Duration of the warm-up phase (can be null)
 * @param timeout Timeout for read/write operations (can be null)
 */
public record BenchmarkRecord(
    int threads,
    Duration duration,
    int rate,
    int connections,
    String urlBase,
    Duration warmUpDuration,
    Duration timeout,
    LoadStrategy mode
) {
    
    /**
     * Compact constructor with validation
     */
    public BenchmarkRecord {
        if (threads <= 0) {
            throw new IllegalArgumentException("threads must be greater than 0, got: " + threads);
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive and non-null");
        }
        if (connections <= 0) {
            throw new IllegalArgumentException("connections must be greater than 0, got: " + connections);
        }
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException("urlBase must not be null or blank");
        }
        if (warmUpDuration != null && warmUpDuration.isNegative()) {
            throw new IllegalArgumentException("warmUpDuration must not be negative");
        }
        if (timeout != null && timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null or blank");
        }
    }
    
    /**
     * Returns true if warm-up phase is configured
     */
    public boolean hasWarmUp() {
        return warmUpDuration != null && !warmUpDuration.isZero();
    }
    
    /**
     * Returns true if timeout is configured
     */
    public boolean hasTimeout() {
        return timeout != null && !timeout.isZero();
    }
    
    /**
     * Calculates the expected maximum number of requests during warm-up phase
     */
    public long expectedWarmUpRequests() {
        return (long) rate * warmUpDuration.getSeconds();
    }
    
    /**
     * Calculates the expected maximum number of requests during test phase
     */
    public long expectedTestRequests() {
        return (long) rate * duration.getSeconds();
    }
    
    /**
     * Calculates requests per second per connection
     */
    public double requestsPerSecondPerConnection() {
        return (double) rate / connections;
    }
}
