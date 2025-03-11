package com.github.yewyc;

import org.jboss.logging.Logger;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.Trace;
import tech.tablesaw.plotly.traces.TraceBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class MeasureLatency {

    private static final Logger LOGGER = Logger.getLogger(MeasureLatency.class);

    private final List<Task> tasks = new ArrayList<>();

    private final int virtualThreads;
    private final long timeNs;
    private final long intervalNs;
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

        this.warmUp();

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
                    if (MeasureLatencyType.GLOBAL.equals(this.latencyType)) {
                        task.recordValue(end - intendedTime);
                    } else if(MeasureLatencyType.INDIVIDUAL.equals(this.latencyType)) {
                        task.recordValue(end - intendedTime - taskElapsed);
                        taskElapsed = end - taskStarted;
                    } else {
                        throw new RuntimeException(this.latencyType + " not implemented");
                    }
                }
            }
        };

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.virtualThreads; i++) {
                executor.submit(wrapperTask);
            }
        } // The executor automatically shuts down here
        return this;
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
                // xAxis and yAxis diff names are required to have different sub-plots
                TraceBuilder traceBuilder = task.plot().xAxis("x" + i).yAxis("y" + i);
                traces.add(build(traceBuilder));
                i += 1;
            }
        }
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().title("Latency(ms) - LatencyType::" + this.latencyType.toString()).grid(grid).build();
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

    private Trace build(TraceBuilder traceBuilder) {
        Method method;
        try {
            method = traceBuilder.getClass().getDeclaredMethod("build");
            method.setAccessible(true);
            Trace trace = (Trace) method.invoke(traceBuilder);
            return trace;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Problem invoking the build method", e);
        }
    }
}