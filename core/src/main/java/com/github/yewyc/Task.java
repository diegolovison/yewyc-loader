package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.jboss.logging.Logger;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class Task implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Task.class);

    private final String name;
    private transient final Runnable action;
    private final boolean trackData;
    private Histogram latencyHistogram;
    private long blockedTime = 0;

    private List<Double> xData = new ArrayList<>();
    private List<Double> yData = new ArrayList<>();

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

    public void recordValue(long when, long value) {
        this.latencyHistogram.recordValue(value);
        if (this.trackData) {
            // this should be also done by some interval to reduce the amount of data collected
            this.xData.add(when / 1.0);
            // value store in ms
            this.yData.add(value / 1_000_000.0);
        }
    }

    /**
     *
     * @param value ns
     */
    public void addBlockedTime(long value) {
        this.blockedTime += value;
    }

    public void report(long intervalNs) {
        System.out.println("Task: " + this.getName());
        System.out.println("\t======= Latency =======");
        System.out.println("\tAverageLatency(ns): " + latencyHistogram.getMean());
        System.out.println("\tMinLatency(ns): " + latencyHistogram.getMinValue());
        System.out.println("\tMaxLatency(ns): " + latencyHistogram.getMaxValue());
        System.out.println("\t50thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(50) / 1_000_000.0);
        System.out.println("\t90thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(90) / 1_000_000.0);
        System.out.println("\t95thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(95) / 1_000_000.0);
        System.out.println("\t99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99) / 1_000_000.0);
        System.out.println("\t99.9thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.9) / 1_000_000.0);
        System.out.println("\t99.99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.99) / 1_000_000.0);
        System.out.println("\t======== Info ========");
        System.out.println("\tTotal requests: " + this.getXData().size());
        System.out.println("\tHistogram total requests: " + latencyHistogram.getTotalCount());
        System.out.println("\tDuration: " + ((this.getXData().getLast() - this.getXData().getFirst()) / 1_000_000_000.0) + " sec");
        System.out.println("\tMean: " + (latencyHistogram.getMean() / 1_000_000.0) + " ms");
        System.out.println("\tBlocked time: " + (blockedTime / 1_000_000.0) + " ms");
        System.out.println("\tStd Dev: " + (latencyHistogram.getStdDeviation() / 1_000_000.0) + " ms");
        System.out.println("\tYou can " + ((latencyHistogram.getMean() < intervalNs) ? "increase" : "decrease") + " the opsPerSec by a factor of: " + String.format("%.2f", intervalNs / latencyHistogram.getMean()));
        System.out.println("\t---");
    }

    public Trace plot(int chartIndex) {
        if (!this.trackData) {
            throw new RuntimeException("Cannot plot because trackData is disabled. Set trackData to true in order to plot the data.");
        }
        double[] xData = this.xData.stream().mapToDouble(Double::doubleValue).toArray();
        double[] yData = this.yData.stream().mapToDouble(Double::doubleValue).toArray();
        ScatterTrace trace = ScatterTrace.builder(xData, yData)
                .mode(ScatterTrace.Mode.LINE)
                .name(this.getName())
                .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                .fill(TO_ZERO_Y)
                .build();
        return trace;
    }

    public List<Double> getXData() {
        return Collections.unmodifiableList(xData);
    }

    public List<Double> getYData() {
        return Collections.unmodifiableList(yData);
    }
}
