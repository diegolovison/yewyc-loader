package com.github.yewyc;

import org.jboss.logging.Logger;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.Trace;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Benchmark implements Closeable {

    private static final Logger log = Logger.getLogger(Benchmark.class);

    protected final List<WeightTask> weightTasks = new ArrayList<>();

    private final int virtualThreads;
    private final long timeSec;
    protected final long intervalNs;
    private final long warmUpTimeSec;

    public Benchmark(long timeSec, int opsPerSec, int virtualThreads, long warmUpTimeSec) {
        if (virtualThreads <= 0) {
            throw new RuntimeException("virtualThreads must be greater than 0");
        }
        if (opsPerSec <= 0) {
            throw new RuntimeException("opsPerSec must be greater than 0");
        }
        if (warmUpTimeSec <= 0) {
            throw new RuntimeException("warmUpTimeSec must be greater than 0");
        }
        this.virtualThreads = virtualThreads;
        this.timeSec = timeSec;
        this.intervalNs = 1000000000 / (opsPerSec / virtualThreads);
        this.warmUpTimeSec = warmUpTimeSec;
    }

    public Benchmark addTask(WeightTask... tasks) {
        for (WeightTask task : tasks) {
            this.weightTasks.add(task);
        }
        return this;
    }

    public Benchmark addTask(List<WeightTask> weightTasks) {
        this.weightTasks.addAll(weightTasks);
        return this;
    }

    public Benchmark start() {
        double[] probabilities = this.weightTasks.stream()
                .mapToDouble(WeightTask::getProbability)
                .toArray();
        double sum = Arrays.stream(probabilities).sum();
        if (sum > 1.0) {
            throw new IllegalStateException("The sum of the probabilities cannot be greater than 1.0");
        }
        log.info("Starting the benchmark");
        run(TimeUnit.SECONDS.toNanos(warmUpTimeSec) + TimeUnit.SECONDS.toNanos(timeSec), probabilities);
        return this;
    }

    private Benchmark run(long durationNs, double[] probabilities) {

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            distributeTasks(durationNs, executor, probabilities);
        } // The executor automatically shuts down here

        log.info("Main executor finished");

        return this;
    }

    private void distributeTasks(long durationNs, ExecutorService executor, double[] probabilities) {

        for (int i = 0; i < this.virtualThreads; i++) {
            executor.submit(new RunnableTask(intervalNs, this.weightTasks, durationNs, probabilities));
        }
    }

    public Benchmark generateReport() {
        for (WeightTask weightTask : this.weightTasks) {
            weightTask.getTask().report(this.intervalNs);
        }
        return this;
    }

    public Benchmark plot() {
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (WeightTask weightTask : this.weightTasks) {
            Task task = weightTask.getTask();
            PlotData plotData = task.plot(i);
            traces.add(plotData.trace);
            i += 1;
        }
        return this.plot(traces);
    }

    protected Benchmark plot(List<Trace> traces) {
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().width(1700).height(800).title("Latency report on milli second").grid(grid).build();
            Figure figure = new Figure(layout, traces.stream().toArray(Trace[]::new));
            Plot.show(figure, new File("/tmp/html.html"));
        }
        return this;
    }

    @Override
    public void close() {

    }
}