package com.github.yewyc;

import org.HdrHistogram.Histogram;

public class Task {

    private final String name;
    private final Runnable action;
    private Histogram latencyHistogram;

    public Task(String name, Runnable action) {
        this.name = name;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void run() {
        this.action.run();
    }

    public void configure(long timeNs) {
        this.latencyHistogram = new Histogram(timeNs, 2);
    }

    public void recordValue(long value) {
        this.latencyHistogram.recordValue(value);
    }

    public void report(long intervalNs) {
        System.out.println("Task: " + this.getName());
        System.out.println("\t======= Latency =======");
        System.out.println("\tOperations: " + latencyHistogram.getTotalCount());
        System.out.println("\tAverageLatency(ns): " + latencyHistogram.getMean());
        System.out.println("\tMinLatency(ns): " + latencyHistogram.getMinValue());
        System.out.println("\tMaxLatency(ns): " + latencyHistogram.getMaxValue());
        System.out.println("\t90thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(90) / 1000000.0);
        System.out.println("\t95thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(95) / 1000000.0);
        System.out.println("\t99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99) / 1000000.0);
        System.out.println("\t99.9thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.9) / 1000000.0);
        System.out.println("\t99.99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.99) / 1000000.0);
        System.out.println("\t---");
        System.out.println("\tYou can " + ((latencyHistogram.getMean() < intervalNs) ? "increase" : "decrease") + " the opsPerSec by a factor of: " + String.format("%.2f", intervalNs / latencyHistogram.getMean()));
        System.out.println("\t---");
    }

}
