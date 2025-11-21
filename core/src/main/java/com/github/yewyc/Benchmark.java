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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Benchmark implements Closeable {

    private static final Logger log = Logger.getLogger(Benchmark.class);

    protected final List<WeightTask> weightTasks = new ArrayList<>();

    private final int threads;
    private final long duration;
    private final int rate;
    private final long warmUpDuration;
    private final boolean recordWarmUp;

    private final Map<String, Statistics> taskMap = new HashMap<>();

    public Benchmark(long duration, int threads) {
        this(duration, threads, Model.CLOSED_MODEL.value, 0, false);
    }

    public Benchmark(long duration, int threads, int rate) {
        this(duration, threads, rate, 0, false);
    }

    /**
     *
     * @param duration
     * @param rate When rate is -1 it will be a closed model. Use the constructor without the rate parameter for convenience
     * @param threads
     * @param warmUpDuration
     */
    public Benchmark(long duration, int threads, int rate, long warmUpDuration, boolean recordWarmUp) {
        if (threads <= 0) {
            throw new RuntimeException("virtualThreads must be greater than 0");
        }
        this.threads = threads;
        this.duration = duration;
        this.rate = rate;
        this.warmUpDuration = warmUpDuration;
        this.recordWarmUp = recordWarmUp;
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
        long intervalNs;
        if (this.rate == Model.CLOSED_MODEL.value) {
            intervalNs = Model.CLOSED_MODEL.value;
        } else {
            intervalNs = 1000000000 / (this.rate / this.threads);
        }

        log.info("Starting the benchmark");
        List<Future<RunnableResult>> tasks = new ArrayList<>();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.threads; i++) {
                Callable<RunnableResult> task = new RunnableTask(i,
                        intervalNs,
                        this.weightTasks,
                        TimeUnit.SECONDS.toNanos(this.warmUpDuration),
                        TimeUnit.SECONDS.toNanos(this.duration),
                        this.recordWarmUp
                );
                Future<RunnableResult> futureTask = executor.submit(task);
                tasks.add(futureTask);
            }
            log.info("Waiting for tasks to complete");
            List<RunnableResult> executedTasks = new ArrayList<>();
            for (Future<RunnableResult> future : tasks) {
                RunnableResult result;
                try {
                    result = future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Benchmark stopped", e);
                }
                executedTasks.add(result);
            }
            log.info("Merging tasks stats");
            // each task as an id
            for (RunnableResult result : executedTasks) {
                for (InstanceTask instanceTask : result.getInstanceTasks()) {
                    Task task = instanceTask.getTask();
                    if (this.taskMap.containsKey(task.getId())) {
                        this.taskMap.get(task.getId()).merge(task.stats(result.getStart(), result.getEnd()));
                    } else {
                        this.taskMap.put(task.getId(), task.stats(result.getStart(), result.getEnd()));
                    }
                }
            }
        } // The executor automatically shuts down here
        log.info("Benchmark finished");
        return this;
    }

    public Benchmark generateReport() {
        this.generateReport(Statistics::printStatistics);
        return this;
    }

    public Benchmark generateReport(Consumer<Statistics> consumer) {
        if (this.taskMap.isEmpty()) {
            throw new RuntimeException("No tasks have been executed");
        }
        for (Statistics stats : this.taskMap.values()) {
            consumer.accept(stats);
        }
        return this;
    }

    public Benchmark plot() {
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (Statistics stats : this.taskMap.values()) {
            // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
            PlotData plotData = stats.plot(i);
            traces.add(plotData.trace);
            i += 1;
        }
        return this.plot(traces);
    }

    protected Benchmark plot(List<Trace> traces) {
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().width(1700).height(800).title("Response time mean(ms)").grid(grid).build();
            Figure figure = new Figure(layout, traces.stream().toArray(Trace[]::new));
            Plot.show(figure, new File("/tmp/report-" + UUID.randomUUID() + ".html"));
        }
        return this;
    }

    @Override
    public void close() {
    }
}
