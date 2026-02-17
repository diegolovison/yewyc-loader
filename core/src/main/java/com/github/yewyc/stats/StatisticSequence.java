package com.github.yewyc.stats;

import java.util.List;

public class StatisticSequence {

    private final Statistic sequenceStatistic;
    private final List<Statistic> stepStatistics;

    public StatisticSequence(Statistic sequenceStatistic, List<Statistic> stepStatistics) {
        this.sequenceStatistic = sequenceStatistic;
        this.stepStatistics = stepStatistics;
    }

    public Statistic getStat() {
        return sequenceStatistic;
    }

    public List<Statistic> getStepStatistics() {
        return stepStatistics;
    }
}
