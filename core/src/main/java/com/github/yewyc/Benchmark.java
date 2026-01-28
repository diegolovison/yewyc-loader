package com.github.yewyc;

import com.github.yewyc.stats.Statistics;
import com.github.yewyc.stats.StatisticsPlot;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.Trace;

import java.io.Closeable;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Benchmark implements Closeable {

    private final int threads;
    private final Duration duration;
    private final int rate;
    private final int connections;
    private final String urlBase;
    private final Duration warmUpDuration;
    private final Duration timeout;

    private final List<Statistics> tasks = new ArrayList<>();

    /**
     * Use the BenchmarkBuilder to construct the object
     *
     * @param rate When rate is 0 it will be a closed model. Operations per second
     */
    Benchmark(int threads, Duration duration, int rate, int connections, String urlBase, Duration warmUpDuration, Duration timeout) {
        if (threads <= 0) {
            throw new RuntimeException("virtualThreads must be greater than 0");
        }
        this.threads = threads;
        this.duration = duration;
        this.rate = rate;
        this.connections = connections;
        this.urlBase = urlBase;
        this.warmUpDuration = warmUpDuration;
        this.timeout = timeout;
    }

    public Benchmark start() {
        BenchmarkRun r = new BenchmarkRun();
        List<Statistics> phaseTasks = r.run(this.rate, this.connections, this.threads, this.urlBase, this.duration, this.warmUpDuration, this.timeout);
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
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (Statistics stats : tasks) {
            PlotData plotData = StatisticsPlot.plot(stats, i);
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
