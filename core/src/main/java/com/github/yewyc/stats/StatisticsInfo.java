package com.github.yewyc.stats;

import org.HdrHistogram.Histogram;

import java.util.List;

class StatisticsInfo {

    final String name;
    final long start;
    final long end;
    final List<Histogram> histograms;
    final List<Integer> errors;

    public StatisticsInfo(String name, long start, long end, List<Histogram> histograms, List<Integer> errors) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.histograms = histograms;
        this.errors = errors;
    }
}
