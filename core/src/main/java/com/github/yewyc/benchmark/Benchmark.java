package com.github.yewyc.benchmark;

import com.github.yewyc.plot.StatisticsPlot;
import com.github.yewyc.stats.Statistics;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Benchmark implements Closeable {

    private final BenchmarkRecord record;

    private final List<Statistics> tasks = new ArrayList<>();

    public Benchmark(BenchmarkRecord record) {
        this.record = record;
    }

    public Benchmark start() {
        BenchmarkRun r = new BenchmarkRun();
        List<Statistics> phaseTasks = r.run(this.record);
        for (Statistics phaseTask : phaseTasks) {
            this.tasks.add(phaseTask);
        }
        return this;
    }

    public Benchmark generateReport(Consumer<Statistics> consumer) {
        if (tasks.isEmpty()) {
            throw new RuntimeException("No tasks have been executed");
        }
        for (Statistics stats : tasks) {
            consumer.accept(stats);
        }
        return this;
    }

    public Benchmark plot() {
        StatisticsPlot.plot(tasks);
        return this;
    }

    @Override
    public void close() {
    }

    public BenchmarkRecord getRecord() {
        return record;
    }
}
