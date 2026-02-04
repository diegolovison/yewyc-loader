package com.github.yewyc.stats;

import org.HdrHistogram.Histogram;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.yewyc.stats.Statistic.highestTrackableValue;
import static com.github.yewyc.stats.Statistic.numberOfSignificantValueDigits;
import static com.github.yewyc.stats.Statistic.oneSecInNanos;

public class SequentialTimeSeriesRecorder {

    private static class Bucket {
        int errors;
        final Histogram histogram;

        public Bucket() {
            // Pre-allocate to avoid GC in the event loop
            this.histogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
            this.histogram.setAutoResize(true);
        }
    }

    private final Bucket[] buffer;
    private long startNanoTime;
    private int cachedIndex = 0;
    private long nextSecondBarrier; // The nano timestamp when the current index expires

    /**
     * @param testDuration The expected duration of the test. The buffer will be fixed to this size. It is a slow operation.
     * you must call start. Be aware of it.
     */
    public SequentialTimeSeriesRecorder(Duration testDuration) {
        this.buffer = new Bucket[Math.toIntExact(testDuration.toSeconds())];
        for (int i = 0; i < testDuration.toSeconds(); i++) {
            buffer[i] = new Bucket();
        }
    }

    public void start(long startNanoTime) {
        this.startNanoTime = startNanoTime;
        this.nextSecondBarrier = startNanoTime + oneSecInNanos;
    }

    public void recordValue(long currentNanoTime, long duration, boolean success) {
        int index;

        // FAST PATH: Most calls hit this.
        // We only check if "currentNanoTime" has passed the barrier.
        // This is a single CPU instruction (comparison).
        if (currentNanoTime < nextSecondBarrier) {
            index = cachedIndex;
        }
        // SLOW PATH: We crossed the second boundary (happens only once per second)
        else {
            long elapsedNanos = currentNanoTime - startNanoTime;

            // Perform the expensive division only when necessary
            index = (int) (elapsedNanos / oneSecInNanos);

            // Update the cache
            cachedIndex = index;

            // Calculate the next barrier.
            // We use (index + 1) to find the start of the NEXT second.
            this.nextSecondBarrier = startNanoTime + ((index + 1) * oneSecInNanos);
        }
        // can have 1 nanosecond diff. the bucked is from when it started. it is fine add as the last one
        if (index >= buffer.length) {
            index = buffer.length - 1;
        }
        Bucket bucket = buffer[index];
        if (!success) {
            bucket.errors++;
        }
        bucket.histogram.recordValue(duration);
    }

    public List<Histogram> getHistograms() {
        List<Histogram> histograms = new ArrayList<>();
        for (int i = 0; i < buffer.length; i++) {
            Bucket b = buffer[i];
            histograms.add(b.histogram);
        }
        return histograms;
    }

    public List<Integer> getErrors() {
        List<Integer> errors = new ArrayList<>();
        for (int i = 0; i < buffer.length; i++) {
            Bucket b = buffer[i];
            errors.add(b.errors);
        }
        return errors;
    }
}
