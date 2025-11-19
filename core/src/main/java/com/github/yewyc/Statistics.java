package com.github.yewyc;

import org.HdrHistogram.Histogram;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.ArrayList;
import java.util.List;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class Statistics {

    private final String name;
    private final Histogram histogram;
    private long firstRecordedTime;
    private long lastRecordedTime;
    private long totalRequests;
    private int errors;
    private final double[] xData;
    private final double[] yData;
    // plot
    private final List<double[]> xDataList;
    private final List<double[]> yDataList;

    public Statistics(String name, Histogram histogram, long firstRecordedTime, long lastRecordedTime,
                      long totalRequests, int errors, double[] xData, double[] yData) {
        this.name = name;
        this.histogram = histogram;
        this.firstRecordedTime = firstRecordedTime;
        this.lastRecordedTime = lastRecordedTime;
        this.totalRequests = totalRequests;
        this.errors = errors;
        this.xData = xData;
        this.yData = yData;

        this.xDataList = new ArrayList<>();
        this.xDataList.add(xData);

        this.yDataList = new ArrayList<>();
        this.yDataList.add(yData);
    }

    public void merge(Statistics stats) {
        if (stats.firstRecordedTime < this.firstRecordedTime) {
            this.firstRecordedTime = stats.firstRecordedTime;
        }
        if (stats.lastRecordedTime > this.lastRecordedTime) {
            this.lastRecordedTime = stats.lastRecordedTime;
        }
        this.histogram.add(stats.getHistogram());
        this.totalRequests += stats.getTotalRequests();
        this.errors += stats.errors;
        this.xDataList.add(this.xData);
        this.yDataList.add(this.yData);
    }

    public Histogram getHistogram() {
        return this.histogram;
    }

    public long getTotalRequests() {
        return this.totalRequests;
    }

    public void printStatistics() {
        double toMs = 1_000_000.0;
        long duration = this.lastRecordedTime - this.firstRecordedTime;
        System.out.println("Task: " + this.name);
        System.out.println("\t======= Response time =======");
        System.out.println("\t50thPercentile(ms): " + this.histogram.getValueAtPercentile(50) / toMs);
        System.out.println("\t90thPercentile(ms): " + this.histogram.getValueAtPercentile(90) / toMs);
        System.out.println("\t95thPercentile(ms): " + this.histogram.getValueAtPercentile(95) / toMs);
        System.out.println("\t99thPercentile(ms): " + this.histogram.getValueAtPercentile(99) / toMs);
        System.out.println("\t99.9thPercentile(ms): " + this.histogram.getValueAtPercentile(99.9) / toMs);
        System.out.println("\t99.99thPercentile(ms): " + this.histogram.getValueAtPercentile(99.99) / toMs);
        System.out.println("\t======== Info ========");
        System.out.println("\tMin(ms): " + (this.histogram.getMinValue() / toMs));
        System.out.println("\tMax(ms): " + (this.histogram.getMaxValue() / toMs));
        System.out.println("\tMean(ms): " + (this.histogram.getMean() / toMs));
        System.out.println("\tStd Dev(ms): " + (this.histogram.getStdDeviation() / toMs));
        System.out.println("\tTotal requests: " + totalRequests);
        System.out.println("\tTotal errors: " + errors);
        System.out.println("\tDuration(sec): " + (duration / 1000));
        System.out.println("\t---");
    }

    public PlotData plot(int chartIndex) {
        int maxTotal = 0;
        for (int i = 0; i < xDataList.size(); i++) {
            if (maxTotal < xDataList.get(i).length) {
                maxTotal = xDataList.get(i).length;
            }
        }
        double[] xData = new double[maxTotal];
        double[] yData = new double[maxTotal];
        for (int i = 0; i < maxTotal; i++) {
            xData[i] = (double) i + 1;
            double meanSum = 0;
            int total = 0;
            for (int j = 0; j < yDataList.size(); j++) {
                if (i < yDataList.get(j).length) {
                    meanSum += yDataList.get(j)[i];
                    total += 1;
                }
            }
            if (total > 0) {
                yData[i] = meanSum / total;
            } else {
                yData[i] = 0.0;
            }

        }
        ScatterTrace trace = ScatterTrace.builder(xData, yData)
                .mode(ScatterTrace.Mode.LINE)
                .name(this.name)
                .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                .fill(TO_ZERO_Y)
                .build();
        return new PlotData(xData, yData, trace);
    }
}
