package com.github.yewyc.benchmark;

import java.time.Duration;

public class BenchmarkBuilder {

    private int threads = -1;
    private Duration duration = null;
    private int connections = -1;
    private String urlBase;
    private int rate;
    private Duration warmUpDuration;
    private Duration timeout;

    public BenchmarkBuilder threads(int threads) {
        this.threads = threads;
        return this;
    }

    public BenchmarkBuilder duration(int duration) {
        this.duration = Duration.ofSeconds(duration);
        return this;
    }

    public BenchmarkBuilder connections(int connections) {
        this.connections = connections;
        return this;
    }

    public BenchmarkBuilder urlBase(String urlBase) {
        this.urlBase = urlBase;
        return this;
    }

    public BenchmarkBuilder rate(int rate) {
        this.rate = rate;
        return this;
    }

    public BenchmarkBuilder warmUpDuration(int warmUpDuration) {
        this.warmUpDuration = Duration.ofSeconds(warmUpDuration);
        return this;
    }

    public BenchmarkBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Benchmark build() {
        if (threads < 0) {
            throw new IllegalStateException("threads must be >= 0");
        }
        if (duration == null || duration.getSeconds() == 0) {
            throw new IllegalStateException("duration must be >= 0");
        }
        if (connections < 0) {
            throw new IllegalStateException("connections must be >= 0");
        }
        if (urlBase == null) {
            throw new IllegalStateException("urlBase must not be null");
        }
        return new Benchmark(new BenchmarkRecord(threads, duration, rate, connections, urlBase, warmUpDuration, timeout));
    }
}
