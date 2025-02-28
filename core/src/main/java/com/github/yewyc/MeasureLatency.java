package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class MeasureLatency {

    private static final Logger LOGGER = Logger.getLogger(MeasureLatency.class);

    private Histogram latencyHistogram;
    private long timeNs;
    private long intervalNs;
    private int virtualThreads;

    public MeasureLatency configure(long timeSec, int opsPerSec) {
        return this.configure(timeSec, opsPerSec, 1);
    }

    public MeasureLatency configure(long timeSec, int opsPerSec, int virtualThreads) {
        if (virtualThreads < 0) {
            throw new RuntimeException("virtualThreads must be equals or greater than 1");
        }
        if (opsPerSec < 0) {
            throw new RuntimeException("opsPerSec must be equals or greater than 0");
        } else if (opsPerSec == 0) {
            LOGGER.warn("Are you aware of what you are doing?");
        }
        this.timeNs = TimeUnit.SECONDS.toNanos(timeSec);
        this.latencyHistogram = new Histogram(timeNs, 2);
        if (opsPerSec > 0) {
            this.intervalNs = 1000000000 / opsPerSec;
        }
        this.virtualThreads = virtualThreads;
        return this;
    }

    public MeasureLatency measure(Runnable runnable) {

        Runnable task = () -> {
            int i = 0;
            long start = System.nanoTime();
            while (true) {

                long intendedTime;
                if (this.intervalNs > 0) {
                    intendedTime = start + (i++) * intervalNs;
                    long now;
                    while ((now = System.nanoTime()) < intendedTime)
                        LockSupport.parkNanos(intendedTime - now);
                } else {
                    intendedTime = System.nanoTime();
                }

                // request
                runnable.run();

                if (System.nanoTime() - start < timeNs) {
                    long end = System.nanoTime();
                    this.latencyHistogram.recordValue(end - intendedTime);
                } else {
                    break;
                }
            }
        };

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.virtualThreads; i++) {
                executor.submit(task);
            }
        } // The executor automatically shuts down here
        return this;
    }

    public void generateReport() {
        // Data is stored in nanoseconds
        System.out.println("======= Latency =======");
        System.out.println("Operations: " + latencyHistogram.getTotalCount());
        System.out.println("AverageLatency(ns): " + latencyHistogram.getMean());
        System.out.println("MinLatency(ns): " + latencyHistogram.getMinValue());
        System.out.println("MaxLatency(ns): " + latencyHistogram.getMaxValue());
        System.out.println("90thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(90) / 1000000.0);
        System.out.println("95thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(95) / 1000000.0);
        System.out.println("99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99) / 1000000.0);
        System.out.println("99.9thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.9) / 1000000.0);
        System.out.println("99.99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.99) / 1000000.0);
        System.out.println("---");
        System.out.println("You can " + ((latencyHistogram.getMean() < this.intervalNs) ? "increase" : "decrease") + " the opsPerSec by a factor of: " + String.format("%.2f", this.intervalNs / latencyHistogram.getMean()));
    }
}