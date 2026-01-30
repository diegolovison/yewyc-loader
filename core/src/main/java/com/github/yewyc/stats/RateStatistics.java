package com.github.yewyc.stats;

public class RateStatistics {
    public final double mean;
    public final double max;
    public final double stdDev;
    public final double pctWithinStdev;
    public final long totalSum;  // Sum of all values (useful for throughput totals)
    public final long totalCount; // Number of recorded items

    protected RateStatistics(double mean, double max, double stdDev, double pctWithinStdev, long totalSum, long totalCount) {
        this.mean = mean;
        this.max = max;
        this.stdDev = stdDev;
        this.pctWithinStdev = pctWithinStdev;
        this.totalSum = totalSum;
        this.totalCount = totalCount;
    }
}
