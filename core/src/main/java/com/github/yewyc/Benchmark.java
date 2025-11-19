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
import java.util.concurrent.TimeUnit;

public class Benchmark implements Closeable {

    private static final Logger log = Logger.getLogger(Benchmark.class);

    protected final List<WeightTask> weightTasks = new ArrayList<>();

    private final int threads;
    private final long duration;
    private final int rate;
    private final long warmUpDuration;
    private final boolean recordWarmUp;

    public Benchmark(long duration, int threads) {
        this(duration, -1, threads, (long) (duration * 0.2));
    }

    public Benchmark(long duration, int threads, long warmUpDuration) {
        this(duration, -1, threads, warmUpDuration);
    }

    /**
     *
     * @param duration
     * @param rate When rate is -1 it will be a closed model. Use the constructor without the rate parameter for convenience
     * @param threads
     * @param warmUpDuration
     */
    public Benchmark(long duration, int rate, int threads, long warmUpDuration) {
        this(duration, rate, threads, warmUpDuration, false);
    }

    public Benchmark(long duration, int rate, int threads, long warmUpDuration, boolean recordWarmUp) {
        if (threads <= 0) {
            throw new RuntimeException("virtualThreads must be greater than 0");
        }
        if (warmUpDuration <= 0) {
            throw new RuntimeException("warmUpTimeSec must be greater than 0");
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

        double[] probabilities = this.weightTasks.stream()
                .mapToDouble(WeightTask::getProbability)
                .toArray();
        double sum = Arrays.stream(probabilities).sum();
        if (sum > 1.0) {
            throw new IllegalStateException("The sum of the probabilities cannot be greater than 1.0");
        }

        log.info("Starting the benchmark");
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.threads; i++) {
                executor.submit(
                        new RunnableTask(
                                intervalNs,
                                this.weightTasks,
                                TimeUnit.SECONDS.toNanos(this.warmUpDuration),
                                TimeUnit.SECONDS.toNanos(this.duration),
                                probabilities,
                                this.recordWarmUp
                        )
                );
            }
        } // The executor automatically shuts down here
        log.info("Benchmark finished");
        return this;
    }

    public Benchmark generateReport() {
        for (WeightTask weightTask : this.weightTasks) {
            weightTask.getTask().report();
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
