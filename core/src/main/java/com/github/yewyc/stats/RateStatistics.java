package com.github.yewyc.stats;

import org.HdrHistogram.AbstractHistogram;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;

public class RateStatistics {
    public final double mean;
    public final double max;
    public final double stdDev;
    public final double pctWithinStdev;
    public final long totalSum;  // Sum of all values (useful for throughput totals)
    public final long totalCount; // Number of recorded items

    private RateStatistics(double mean, double max, double stdDev, double pctWithinStdev, long totalSum, long totalCount) {
        this.mean = mean;
        this.max = max;
        this.stdDev = stdDev;
        this.pctWithinStdev = pctWithinStdev;
        this.totalSum = totalSum;
        this.totalCount = totalCount;
    }

    /**
     * Factory for Throughput (based on a list of counts per second)
     */
    public static RateStatistics fromThroughput(Collection<Long> dataset) {
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

    /**
     * Factory for Latency (based on HdrHistogram)
     */
    public static RateStatistics fromLatency(AbstractHistogram histogram) {
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
}
