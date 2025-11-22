package com.github.yewyc;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Task implements Serializable {

    static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    static final int numberOfSignificantValueDigits = 3;

    private String id;
    private final String name;
    private Recorder recorder;
    private List<Histogram> histograms;
    private boolean started = false;
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

    public abstract void initialize(ExecutorService executor);

    public abstract CompletableFuture<TaskStatus> submit();

    public abstract void close();

    public void recordValue(long elapsedTimeNs, TaskStatus taskStatus) {
        // used to group histograms by second - right now second is fixed
        long now = System.currentTimeMillis();
        if (!this.started) {
            this.started = true;
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

    public Statistics stats(long start, long end) {
        return new Statistics(this.name, start, end, histograms, errors);
    }
}
