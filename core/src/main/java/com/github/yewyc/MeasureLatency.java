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

public class MeasureLatency implements Closeable {

    private static final Logger log = Logger.getLogger(MeasureLatency.class);

    protected final List<WeightTask> weightTasks = new ArrayList<>();

    private final int virtualThreads;
    private final long timeSec;
    protected final long intervalNs;
    private final long warmUpTimeSec;
    private final MeasureLatencyType latencyType;

    public MeasureLatency(long timeSec, int opsPerSec, int virtualThreads, long warmUpTimeSec, MeasureLatencyType latencyType) {
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
        this.latencyType = latencyType;
    }

    public MeasureLatency addTask(Task... tasks) {
        if (this.weightTasks.isEmpty()) {
            this.weightTasks.add(new WeightTask(Arrays.asList(tasks), 1.0));
        } else if (this.weightTasks.size() == 1) {
            this.weightTasks.getFirst().getTasks().addAll(Arrays.asList(tasks));
        } else {
            throw new IllegalStateException("You cannot mix weightTasks with task");
        }
        return this;
    }

    public MeasureLatency addWeightTask(List<WeightTask> weightTasks) {
        this.weightTasks.addAll(weightTasks);
        return this;
    }

    public MeasureLatency start() {
        double[] probabilities = this.weightTasks.stream()
                .mapToDouble(WeightTask::getProbability)
                .toArray();
        double sum = Arrays.stream(probabilities).sum();
        if (sum > 1.0) {
            throw new IllegalStateException("The sum of the probabilities cannot be greater than 1.0");
        }
        log.info("Starting the warm up phase");
        run(TimeUnit.SECONDS.toNanos(warmUpTimeSec), probabilities);
        log.info("Starting the benchmark");
        run(TimeUnit.SECONDS.toNanos(timeSec), probabilities);
        return this;
    }

    private MeasureLatency run(long durationNs, double[] probabilities) {

        for (WeightTask weightTask : this.weightTasks) {
            for (Task task : weightTask.getTasks()) {
                task.configure(durationNs);
            }
        }

        final ThreadPoolExecutor recordExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            distributeTasks(durationNs, executor, recordExecutor, probabilities);
        } // The executor automatically shuts down here

        log.info("Main executor finished");

        try {
            log.info("Waiting for record executor: " + recordExecutor.getQueue().size());
            recordExecutor.shutdown();
            recordExecutor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.warn("More than 1 hour waiting to record the values. Some values may be missing", e);
        }

        log.info("Record executor finished");

        return this;
    }

    private void distributeTasks(long durationNs, ExecutorService executor, ThreadPoolExecutor recordExecutor, double[] probabilities) {

        for (int i = 0; i < this.virtualThreads; i++) {
            executor.submit(new RunnableTask(intervalNs, this.weightTasks, durationNs, latencyType, recordExecutor, probabilities));
        }
    }

    public MeasureLatency generateReport() {
        List<Task> reported = new ArrayList<>();
        for (WeightTask weightTask : this.weightTasks) {
            for (Task task : weightTask.getTasks()) {
                if (!reported.contains(task)) {
                    task.report(this.intervalNs);
                    reported.add(task);
                }
            }
        }
        return this;
    }

    public MeasureLatency plot() {
        List<Task> reported = new ArrayList<>();
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (WeightTask weightTask : this.weightTasks) {
            for (Task task : weightTask.getTasks()) {
                if (task.hasTrackData()) {
                    if (!reported.contains(task)) {
                        traces.add(task.plot(i));
                        i += 1;
                        reported.add(task);
                    }
                }
            }
        }
        return this.plot(traces);
    }

    protected MeasureLatency plot(List<Trace> traces) {
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().width(1700).height(800).title("Latency(ms) - LatencyType::" + this.latencyType.toString()).grid(grid).build();
            Figure figure = new Figure(layout, traces.stream().toArray(Trace[]::new));
            Plot.show(figure, new File("/tmp/html.html"));
        }
        return this;
    }

    @Override
    public void close() {

    }
}