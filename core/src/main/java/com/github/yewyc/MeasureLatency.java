package com.github.yewyc;

import org.jboss.logging.Logger;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.Trace;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class MeasureLatency implements Closeable {

    private static final Logger log = Logger.getLogger(MeasureLatency.class);
    private static final ThreadPoolExecutor recordExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    protected final List<Task> tasks = new ArrayList<>();

    private final int virtualThreads;
    private final long timeNs;
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
        this.timeNs = TimeUnit.SECONDS.toNanos(timeSec);
        this.intervalNs = 1000000000 / opsPerSec;
        this.warmUpTimeSec = warmUpTimeSec;
        this.latencyType = latencyType;
    }

    public MeasureLatency addTask(Task... tasks) {
        for (Task task : tasks) {
            task.configure(this.timeNs);
            this.tasks.add(task);
        }
        return this;
    }

    public MeasureLatency start() {

        log.info("Starting the warm up phase");

        this.warmUp();

        log.info("Starting the benchmark");

        Runnable wrapperTask = () -> {
            int i = 0;
            long start = System.nanoTime();
            outer:
            while (true) {

                // when start
                long intendedTime = start + (i++) * intervalNs;
                long now;
                while ((now = System.nanoTime()) < intendedTime)
                    LockSupport.parkNanos(intendedTime - now);

                // request
                long taskStarted = System.nanoTime();
                long taskElapsed = 0;
                for (int j = 0; j < this.tasks.size(); j++) {
                    Task task = this.tasks.get(j);
                    task.run();
                    long end = System.nanoTime();
                    // stop?
                    if (end - start > timeNs) {
                        break outer;
                    }
                    taskElapsed = record(task, end, intendedTime, taskElapsed, taskStarted);
                }
            }
        };

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.virtualThreads; i++) {
                executor.submit(wrapperTask);
            }
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

    private long record(Task task, long end, long intendedTime, long taskElapsed, long taskStarted) {
        if (MeasureLatencyType.GLOBAL.equals(this.latencyType)) {
            CompletableFuture.runAsync(() -> task.recordValue(end, end - intendedTime), recordExecutor);
        } else if(MeasureLatencyType.INDIVIDUAL.equals(this.latencyType)) {
            long finalTaskElapsed = taskElapsed;
            CompletableFuture.runAsync(() -> task.recordValue(end, end - intendedTime - finalTaskElapsed), recordExecutor);
            taskElapsed = end - taskStarted;
        } else {
            throw new RuntimeException(this.latencyType + " not implemented");
        }
        return taskElapsed;
    }

    public MeasureLatency generateReport() {
        for (Task task : this.tasks) {
            task.report(this.intervalNs);
        }
        return this;
    }

    public MeasureLatency plot() {
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (Task task : this.tasks) {
            if (task.hasTrackData()) {
                traces.add(task.plot(i));
                i += 1;
            }
        }
        return this.plot(traces);
    }

    protected MeasureLatency plot(List<Trace> traces) {
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().width(1700).height(800).title("Latency(ms) - LatencyType::" + this.latencyType.toString()).grid(grid).build();
            Plot.show(new Figure(layout, traces.stream().toArray(Trace[]::new)));
        }
        return this;
    }

    private void warmUp() {
        long warmUpTimeNs = TimeUnit.SECONDS.toNanos(this.warmUpTimeSec);
        Runnable wrapperTask = () -> {
            long start = System.nanoTime();
            outer:
            while (true) {
                // request
                for (int j = 0; j < this.tasks.size(); j++) {
                    this.tasks.get(j).run();
                    long end = System.nanoTime();
                    // stop?
                    if (end - start > warmUpTimeNs) {
                        break outer;
                    }
                }
            }
        };

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.virtualThreads; i++) {
                executor.submit(wrapperTask);
            }
        } // The executor automatically shuts down here
    }

    @Override
    public void close() {

    }
}