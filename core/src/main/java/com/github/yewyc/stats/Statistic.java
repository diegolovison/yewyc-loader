package com.github.yewyc.stats;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Statistic {

    public static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    public static final int numberOfSignificantValueDigits = 3;
    public static final double scale = TimeUnit.MILLISECONDS.toNanos(1);

    private final String name;
    private final long start;
    private final long end;
    private final List<Histogram> histograms;
    private final List<Integer> errors;

    private List<Statistic> all;

    public Statistic(String name, long start, long end, List<Histogram> histograms, List<Integer> errors) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.histograms = histograms;
        this.errors = errors;
        this.all = new ArrayList<>();
    }

    public void merge(Statistic statistic) {
        this.all.add(statistic);
    }

    public RateStatistics getThroughput() {
        Collection<Long> dataset = getTotalRequests();
        if (dataset.isEmpty()) return new RateStatistics(0, 0, 0, 0, 0, 0);

        // 1. Basic Stats
        DoubleSummaryStatistics stats = dataset.stream()
                .mapToDouble(Long::doubleValue)
                .summaryStatistics();

        double mean = stats.getAverage();
        double max = stats.getMax();
        long sum = (long) stats.getSum();
        long count = stats.getCount();

        // 2. Std Dev
        double variance = dataset.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .sum() / count;
        double stdDev = Math.sqrt(variance);

        // 3. % Within Stdev
        long countWithin = dataset.stream()
                .filter(s -> s >= (mean - stdDev) && s <= (mean + stdDev))
                .count();
        double pct = (countWithin * 100.0) / count;

        return new RateStatistics(mean, max, stdDev, pct, sum, count);
    }

    public RateStatistics getLatency() {
        AbstractHistogram histogram = getHistogram();
        if (histogram.getTotalCount() == 0) return new RateStatistics(0, 0, 0, 0, 0, 0);

        // 1. Basic Stats directly from Histogram
        double mean = histogram.getMean(); // Note: HdrHistogram mean is in raw units (nanos/micros)
        double max = histogram.getMaxValue();
        double stdDev = histogram.getStdDeviation();
        long totalCount = histogram.getTotalCount();

        // 2. % Within Stdev (Iterating buckets)
        double lower = mean - stdDev;
        double upper = mean + stdDev;
        long sumWithin = 0;

        for (var value : histogram.allValues()) {
            if (value.getValueIteratedTo() >= lower && value.getValueIteratedTo() <= upper) {
                sumWithin += value.getCountAddedInThisIterationStep();
            }
        }
        double pct = (sumWithin * 100.0) / totalCount;

        return new RateStatistics(mean, max, stdDev, pct, 0, totalCount);
    }

    public double duration() {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        for (Statistic info : this.all) {
            if (info.start < start) {
                start = info.start;
            }
            if (info.end > end) {
                end = info.end;
            }
        }
        return (end - start) / 1000.0;
    }

    public String getName() {
        return this.all.getFirst().name;
    }

    public int getTotalErrors() {
        int totalErrors = 0;
        for (Statistic info : this.all) {
            totalErrors += info.errors.stream().mapToInt(Integer::intValue).sum();
        }
        return totalErrors;
    }

    public double[][] getXY() {
        int maxTotal = Integer.MIN_VALUE;
        for (Statistic info : all) {
            maxTotal = Math.max(maxTotal, info.histograms.size());
        }
        double[] xData = new double[maxTotal];
        double[] yData = new double[maxTotal];
        double[] counter = new double[maxTotal];
        for (int i = 0; i < maxTotal; i++) {
            xData[i] = (double) i + 1;

            Histogram aggregate = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);

            boolean hasData = false;
            for (Statistic info : all) {
                if (i < info.histograms.size()) {
                    Histogram h = info.histograms.get(i);
                    counter[i] += h.getTotalCount();
                    if (h.getTotalCount() > 0) {
                        aggregate.add(h);
                        hasData = true;
                    }
                }
            }
            if (hasData) {
                yData[i] = aggregate.getMean() / scale;
            } else {
                yData[i] = 0.0;
            }
        }
        return new double[][]{xData, yData, counter};
    }

    private AbstractHistogram getHistogram() {
        Histogram latencyHistogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
        for (Statistic info : this.all) {
            for (Histogram histogram : info.histograms) {
                latencyHistogram.add(histogram);
            }
        }
        return latencyHistogram;
    }

    private Collection<Long> getTotalRequests() {
        List<Long> totalRequests = new ArrayList<>();
        for (Statistic info : this.all) {
            for (Histogram histogram : info.histograms) {
                totalRequests.add(histogram.getTotalCount());
            }
        }
        return totalRequests;
    }
}
