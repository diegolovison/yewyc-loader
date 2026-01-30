package com.github.yewyc.stats;

import org.HdrHistogram.Histogram;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Per connection
 */
public class Statistic {

    public static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    public static final int numberOfSignificantValueDigits = 3;
    public static final double scale = TimeUnit.MILLISECONDS.toNanos(1);

    private final List<Histogram> histograms;
    private final List<Integer> errors;

    public Statistic(List<Histogram> histograms, List<Integer> errors) {
        this.histograms = histograms;
        this.errors = errors;
    }

    public List<Histogram> getHistograms() {
        return histograms;
    }
}
