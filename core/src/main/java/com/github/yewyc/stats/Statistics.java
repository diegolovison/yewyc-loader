package com.github.yewyc.stats;

import com.github.yewyc.PlotData;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class Statistics {

    public static final long highestTrackableValue = TimeUnit.MINUTES.toNanos(1);
    public static final int numberOfSignificantValueDigits = 3;
    public static final long scale = TimeUnit.MILLISECONDS.toNanos(1);

    private StatisticsInfo info;
    private List<StatisticsInfo> all;

    public Statistics(String name, long start, long end, List<Histogram> histograms, List<Integer> errors) {
        this.info = new StatisticsInfo(name, start, end, histograms, errors);
        this.all = new ArrayList<>();
        this.all.add(this.info);
    }

    public void merge(Statistics stats) {
        this.all.add(stats.info);
    }

    public Collection<Long> getTotalRequests() {
        List<Long> totalRequests = new ArrayList<>();
        for (StatisticsInfo info : this.all) {
            for (Histogram histogram : info.histograms) {
                totalRequests.add(histogram.getTotalCount());
            }
        }
        return totalRequests;
    }

    public long duration() {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        for (StatisticsInfo info : this.all) {
            if (info.start < start) {
                start = info.start;
            }
            if (info.end > end) {
                end = info.end;
            }
        }
        return (end - start) / 1000;
    }

    public String getName() {
        return this.all.getFirst().name;
    }

    public AbstractHistogram getHistogram() {
        Histogram latencyHistogram = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);
        for (StatisticsInfo info : this.all) {
            for (Histogram histogram : info.histograms) {
                latencyHistogram.add(histogram);
            }
        }
        return latencyHistogram;
    }

    public int getTotalErrors() {
        int totalErrors = 0;
        for (StatisticsInfo info : this.all) {
            totalErrors += info.errors.stream().mapToInt(Integer::intValue).sum();
        }
        return totalErrors;
    }

    public void printStatistics() {
        AbstractHistogram histogram = this.getHistogram();
        long totalRequests = this.getTotalRequests().stream().mapToLong(Long::longValue).sum();
        long totalErrors = this.getTotalErrors();
        System.out.println("Task: " + info.name);
        System.out.println("\t======= Response time =======");
        System.out.println("\t50thPercentile(ms): " + histogram.getValueAtPercentile(50) / scale);
        System.out.println("\t90thPercentile(ms): " + histogram.getValueAtPercentile(90) / scale);
        System.out.println("\t95thPercentile(ms): " + histogram.getValueAtPercentile(95) / scale);
        System.out.println("\t99thPercentile(ms): " + histogram.getValueAtPercentile(99) / scale);
        System.out.println("\t99.9thPercentile(ms): " + histogram.getValueAtPercentile(99.9) / scale);
        System.out.println("\t99.99thPercentile(ms): " + histogram.getValueAtPercentile(99.99) / scale);
        System.out.println("\t======== Info ========");
        System.out.println("\tMin(ms): " + (histogram.getMinValue() / scale));
        System.out.println("\tMax(ms): " + (histogram.getMaxValue() / scale));
        System.out.println("\tMean(ms): " + (histogram.getMean() / scale));
        System.out.println("\tStd Dev(ms): " + (histogram.getStdDeviation() / scale));
        System.out.println("\tTotal requests: " + totalRequests);
        System.out.println("\tTotal errors: " + totalErrors);
        System.out.println("\tDuration(sec): " + this.duration());
        System.out.println("\t---");
    }

    public PlotData plot(int chartIndex) {
        int maxTotal = Integer.MIN_VALUE;
        for (StatisticsInfo info : all) {
            if (info.histograms.size() > maxTotal) {
                maxTotal = info.histograms.size();
            }
        }
        double[] xData = new double[maxTotal];
        double[] yData = new double[maxTotal];
        for (int i = 0; i < maxTotal; i++) {
            xData[i] = (double) i + 1;
            double meanSum = 0;
            long total = 0;

            for (StatisticsInfo info : all) {
                if (i < info.histograms.size()) {
                    Histogram histogram = info.histograms.get(i);
                    meanSum += histogram.getMean();
                    total += histogram.getTotalCount();
                }
            }
            if (total > 0) {
                yData[i] = (meanSum / total) / 1000000;
            } else {
                yData[i] = 0.0;
            }

        }
        ScatterTrace trace = ScatterTrace.builder(xData, yData)
                .mode(ScatterTrace.Mode.LINE)
                .name(this.info.name)
                .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                .fill(TO_ZERO_Y)
                .build();
        return new PlotData(xData, yData, trace);
    }

    private static final class StatisticsInfo {
        private final String name;
        private final long start;
        private final long end;
        private final List<Histogram> histograms;
        private final List<Integer> errors;

        public StatisticsInfo(String name, long start, long end, List<Histogram> histograms, List<Integer> errors) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.histograms = histograms;
            this.errors = errors;
        }
    }
}
