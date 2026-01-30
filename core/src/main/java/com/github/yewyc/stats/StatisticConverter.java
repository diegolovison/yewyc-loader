package com.github.yewyc.stats;

import org.HdrHistogram.Histogram;

import java.util.ArrayList;
import java.util.List;

import static com.github.yewyc.stats.Statistic.highestTrackableValue;
import static com.github.yewyc.stats.Statistic.numberOfSignificantValueDigits;

public class StatisticConverter {

    public static List<StatisticTick> convert(List<Statistic> stats) {
        List<StatisticTick> result = new ArrayList<>();
        int maxTotal = Integer.MIN_VALUE;
        for (Statistic info : stats) {
            maxTotal = Math.max(maxTotal, info.getHistograms().size());
        }
        for (int i = 0; i < maxTotal; i++) {
            Histogram latency = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
            long counter = 0;
            for (Statistic info : stats) {
                if (i < info.getHistograms().size()) {
                    Histogram h = info.getHistograms().get(i);
                    if (h.getTotalCount() > 0) {
                        latency.add(h);
                    }
                    counter += h.getTotalCount();
                }
            }
            result.add(new StatisticTick(latency, counter));
        }
        return result;
    }
}
