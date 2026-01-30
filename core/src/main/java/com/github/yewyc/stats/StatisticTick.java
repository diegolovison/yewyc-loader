package com.github.yewyc.stats;

import org.HdrHistogram.AbstractHistogram;

public record StatisticTick(AbstractHistogram latency, long counter) {
}
