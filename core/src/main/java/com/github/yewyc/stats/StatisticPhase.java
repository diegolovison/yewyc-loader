package com.github.yewyc.stats;

import org.HdrHistogram.Histogram;

import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import static com.github.yewyc.stats.Statistic.highestTrackableValue;
import static com.github.yewyc.stats.Statistic.numberOfSignificantValueDigits;

public class StatisticPhase {

    private final String name;
    private final Duration phaseDuration;
    private final List<StatisticTick> statisticTicks;

    public StatisticPhase(String name, Duration phaseDuration, List<StatisticTick> statisticTicks) {
        this.name = name;
        this.phaseDuration = phaseDuration;
        this.statisticTicks = statisticTicks;
    }

    public String getName() {
        return this.name;
    }

    public RateStatistics getThroughput() {
        if (statisticTicks.isEmpty()) return new RateStatistics(0, 0, 0, 0, 0, 0);

        // 1. Basic Stats
        DoubleSummaryStatistics stats = statisticTicks.stream()
                .mapToDouble(StatisticTick::counter)
                .summaryStatistics();

        double mean = stats.getAverage();
        double max = stats.getMax();
        long sum = (long) stats.getSum();
        long count = stats.getCount();

        // 2. Std Dev
        double variance = statisticTicks.stream()
                .mapToDouble(s -> Math.pow(s.counter() - mean, 2))
                .sum() / count;
        double stdDev = Math.sqrt(variance);

        // 3. % Within Stdev
        long countWithin = statisticTicks.stream()
                .filter(s -> s.counter() >= (mean - stdDev) && s.counter() <= (mean + stdDev))
                .count();
        double pct = (countWithin * 100.0) / count;

        return new RateStatistics(mean, max, stdDev, pct, sum, count);
    }

    public RateStatistics getLatency() {
        if (statisticTicks.isEmpty()) return new RateStatistics(0, 0, 0, 0, 0, 0);

        Histogram allLatencies = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
        for (StatisticTick statisticTick : statisticTicks) {
            allLatencies.add(statisticTick.latency());
        }

        // 1. Basic Stats directly from Histogram
        double mean = allLatencies.getMean(); // Note: HdrHistogram mean is in raw units (nanos/micros)
        double max = allLatencies.getMaxValue();
        double stdDev = allLatencies.getStdDeviation();
        long totalCount = allLatencies.getTotalCount();

        // 2. % Within Stdev (Iterating buckets)
        double lower = mean - stdDev;
        double upper = mean + stdDev;
        long sumWithin = 0;

        for (var value : allLatencies.allValues()) {
            if (value.getValueIteratedTo() >= lower && value.getValueIteratedTo() <= upper) {
                sumWithin += value.getCountAddedInThisIterationStep();
            }
        }
        double pct = (sumWithin * 100.0) / totalCount;

        return new RateStatistics(mean, max, stdDev, pct, 0, totalCount);
    }

    public Duration duration() {
        return this.phaseDuration;
    }

    public List<StatisticTick> getStatisticTicks() {
        return statisticTicks;
    }
}
