package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class Task implements Serializable {

    private static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    private static final int numberOfSignificantValueDigits = 3;
    private static final long NANO_PER_MS = 1_000_000;

    private String id;
    private final String name;
    private Recorder recorder;
    private List<Histogram> histograms;
    // used to control when the test started
    private long firstRecordedTime = 0;
    private long lastRecordedTime = 0;
    private long lastRecordedTimeForGroupingHistograms = 0;
    private int errorCount = 0;
    // if we wish to track per time bucket - current is second
    private List<Integer> errors;

    public Task(String name) {
        this.name = name;
        // 1. Initialize a Recorder
        // We configure it to track values up to 1 minute (60,000 ms)
        // with 3 significant digits of precision.
        // we are measuring in nanoseconds. if a value stored is greater than a minute, it will auto-resize
        this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
        this.histograms = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public abstract TaskStatus run();

    public void recordValue(long elapsedTimeNs, TaskStatus taskStatus) {
        // used to group histograms by second - right now second is fixed
        // when started
        long now = System.currentTimeMillis();
        if (this.firstRecordedTime == 0) {
            this.firstRecordedTime = now;
            this.lastRecordedTimeForGroupingHistograms = now;
        } else {
            long elapsedTimeForGroupingHistagrams = now - this.lastRecordedTimeForGroupingHistograms;
            if (elapsedTimeForGroupingHistagrams >= 1000) {
                this.histograms.add(recorder.getIntervalHistogram());
                this.errors.add(this.errorCount);
                this.errorCount = 0;
                this.lastRecordedTimeForGroupingHistograms = now;
            }
        }

        // track
        this.recorder.recordValue(elapsedTimeNs);
        if (TaskStatus.FAILED.equals(taskStatus)) {
            this.errorCount++;
        }
        this.lastRecordedTime = now;
    }

    public void setId(String id) {
        if (this.id != null) {
            throw new IllegalStateException("Id cannot be modified");
        }
        this.id = id;
    }

    public String getId() {
        if (this.id == null) {
            throw new IllegalStateException("Id cannot be null");
        }
        return this.id;
    }

    public Statistics stats() {
        Histogram latencyHistogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
        long totalRequests = 0;
        double[] xData;
        double[] yData;
        // too fast
        if (this.histograms.isEmpty()) {
            Histogram localHistogram = this.recorder.getIntervalHistogram();
            latencyHistogram.add(localHistogram);
            totalRequests = localHistogram.getTotalCount();
            xData = new double[1];
            yData = new double[1];
            xData[0] = 1.0;
            yData[0] = this.recorder.getIntervalHistogram().getMean() / NANO_PER_MS;
        } else {
            xData = new double[this.histograms.size()];
            yData = new double[this.histograms.size()];
            for (int i = 0; i < this.histograms.size(); i++) {
                Histogram intervalHistogram = this.histograms.get(i);
                latencyHistogram.add(intervalHistogram);
                totalRequests += intervalHistogram.getTotalCount();
                xData[i] = (double) i + 1;
                yData[i] = intervalHistogram.getMean() / NANO_PER_MS;
            }
        }
        return new Statistics(this.name, latencyHistogram, this.firstRecordedTime, this.lastRecordedTime, totalRequests,
                this.errors.stream().mapToInt(Integer::intValue).sum(), xData, yData);
    }
}
