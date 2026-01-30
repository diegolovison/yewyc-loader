package com.github.yewyc.stats;

import java.time.Duration;

/**
 * Per phase
 */
public class StatisticPhase {

    private final String name;
    private final Duration phaseDuration;
    private final Statistic statistic;

    public StatisticPhase(String name, Duration phaseDuration, Statistic statistic) {
        this.name = name;
        this.phaseDuration = phaseDuration;
        this.statistic = statistic;
    }

    public double[][] getXY() {
        return this.statistic.getXY();
    }

    public String getName() {
        return this.name;
    }

    public RateStatistics getThroughput() {
        return this.statistic.getThroughput();
    }

    public RateStatistics getLatency() {
        return this.statistic.getLatency();
    }

    public Duration duration() {
        return this.phaseDuration;
    }

    public int getTotalErrors() {
        return this.statistic.getTotalErrors();
    }
}
