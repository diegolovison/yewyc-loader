package com.github.yewyc.benchmark;

import com.github.yewyc.plot.StatisticsPlot;
import com.github.yewyc.scenario.ScenarioFactory;
import com.github.yewyc.stats.StatisticPhase;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Benchmark implements Closeable {

    private final BenchmarkRecord record;
    private final List<StatisticPhase> statistics = new ArrayList<>();
    private BenchmarkRun benchmarkRun;

    public Benchmark(BenchmarkRecord record) {
        this.record = record;
    }

    public Benchmark start(ScenarioFactory scenarioFactory) throws InterruptedException {
        if (benchmarkRun != null) {
            throw new IllegalStateException("Benchmark already started");
        }
        this.benchmarkRun = new BenchmarkRun(scenarioFactory);
        this.statistics.addAll(this.benchmarkRun.run(this.record));
        return this;
    }

    public Benchmark generateReport(Consumer<StatisticPhase> consumer) {
        if (statistics.isEmpty()) {
            throw new RuntimeException("No tasks have been executed");
        }
        for (StatisticPhase statistic : statistics) {
            consumer.accept(statistic);
        }
        return this;
    }

    public Benchmark plot() {
        if (statistics.isEmpty()) {
            throw new RuntimeException("No tasks have been executed");
        }
        StatisticsPlot.plot(statistics);
        return this;
    }

    @Override
    public void close() {
    }

    public BenchmarkRecord getRecord() {
        return record;
    }

    public void dump() {
        if (statistics.isEmpty()) {
            throw new RuntimeException("No tasks have been executed");
        }
        this.benchmarkRun.dump();
    }
}
