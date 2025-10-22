package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class Task implements Serializable {

    private static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    private static final int numberOfSignificantValueDigits = 3;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANO_PER_MS = 1_000_000;
    private final String name;
    private transient final Runnable action;
    private Recorder recorder;
    private long blockedTime;
    private List<Histogram> histograms;
    private long firstRecorderData = 0;
    private long lastRecordData = 0;

    public Task(String name, Runnable action) {
        this.name = name;
        this.action = action;
        // 1. Initialize a Recorder
        // We configure it to track values up to 1 minute (60,000 ms)
        // with 3 significant digits of precision.
        // we are measuring in nanoseconds. if a value stored is greater than a minute, it will auto-resize
        this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
        this.histograms = new ArrayList<>();
        this.blockedTime = 0;
    }

    public String getName() {
        return this.name;
    }

    public void run() {
        this.action.run();
    }

    /**
     *
     * @param when value in ns
     * @param value value in ns
     */
    public void recordValue(long when, long value) {
        if (this.firstRecorderData == 0) {
            this.firstRecorderData = when;
        }
        long durationNanos = when - this.lastRecordData;
        boolean hasOneSecondElapsed = durationNanos >= NANOS_PER_SECOND;
        if (hasOneSecondElapsed) {
            if (this.lastRecordData != 0) {
                this.histograms.add(recorder.getIntervalHistogram());
            }
            this.lastRecordData = when;
        }
        this.recorder.recordValue(value);
    }

    /**
     *
     * @param value ns
     */
    public void addBlockedTime(long value) {
        this.blockedTime += value;
    }

    public void report(long intervalNs) {
        Histogram latencyHistogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
        long totalRequests = 0;
        // too fast
        if (this.histograms.isEmpty()) {
            Histogram localHistogram = this.recorder.getIntervalHistogram();
            latencyHistogram.add(localHistogram);
            totalRequests = localHistogram.getTotalCount();
        } else {
            for (Histogram intervalHistogram : this.histograms) {
                latencyHistogram.add(intervalHistogram);
                totalRequests += intervalHistogram.getTotalCount();
            }
        }

        long duration = (this.lastRecordData - this.firstRecorderData) / NANOS_PER_SECOND;
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
        System.out.println("\tTotal requests: " + totalRequests);
        System.out.println("\tTotal histograms: " + this.histograms.size());
        System.out.println("\tDuration: " + duration + " sec");
        System.out.println("\tMean: " + (latencyHistogram.getMean() / 1_000_000.0) + " ms");
        System.out.println("\tBlocked time: " + (blockedTime / 1_000_000.0) + " ms");
        System.out.println("\tStd Dev: " + (latencyHistogram.getStdDeviation() / 1_000_000.0) + " ms");
        System.out.println("\tYou can " + ((latencyHistogram.getMean() < intervalNs) ? "increase" : "decrease") + " the opsPerSec by a factor of: " + String.format("%.2f", intervalNs / latencyHistogram.getMean()));
        System.out.println("\t---");
    }

    public PlotData plot(int chartIndex) {
        Double[] xData = new Double[this.histograms.size()];
        Double[] yData = new Double[this.histograms.size()];
        for (int i = 0; i < xData.length; i++) {
            Histogram intervalHistogram = this.histograms.get(i);
            xData[i] = (double) i + 1;
            yData[i] = intervalHistogram.getMean() / NANO_PER_MS;
        }
        ScatterTrace trace = ScatterTrace.builder(
                        Stream.of(xData).mapToDouble(Double::doubleValue).toArray(),
                        Stream.of(yData).mapToDouble(Double::doubleValue).toArray())
                .mode(ScatterTrace.Mode.LINE)
                .name(this.getName())
                .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                .fill(TO_ZERO_Y)
                .build();
        return new PlotData(xData, yData, trace);
    }
}
