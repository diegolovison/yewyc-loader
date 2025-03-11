package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.jboss.logging.Logger;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.TraceBuilder;

import java.util.ArrayList;
import java.util.List;

public class Task {

    private static final Logger LOGGER = Logger.getLogger(Task.class);

    private final String name;
    private final Runnable action;
    private final boolean trackData;
    private Histogram latencyHistogram;

    private List<Double> data = new ArrayList<>();

    public Task(String name, Runnable action) {
        this(name, action, false);
    }

    public Task(String name, Runnable action, boolean trackData) {
        this.name = name;
        this.action = action;
        this.trackData = trackData;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasTrackData() {
        return this.trackData;
    }

    public void run() {
        this.action.run();
    }

    public void configure(long timeNs) {
        this.latencyHistogram = new Histogram(timeNs, 2);
    }

    public void recordValue(long value) {
        this.latencyHistogram.recordValue(value);
        if (this.trackData) {
            // ns to ms
            this.data.add(value / 1000000.0);
        }
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

    public TraceBuilder plot() {
        if (!this.trackData) {
            LOGGER.warn("Cannot plot because trackData is disabled. Set trackData to true in order to plot the data.");
            return null;
        }
        double[] operations = new double[this.data.size()];
        double[] latency = new double[this.data.size()];
        for (int i = 0; i < this.data.size(); i++) {
            operations[i] = i + 1;
            latency[i] = this.data.get(i);
        }
        return ScatterTrace.builder(operations, latency).mode(ScatterTrace.Mode.LINE).name(this.getName());
    }
}
