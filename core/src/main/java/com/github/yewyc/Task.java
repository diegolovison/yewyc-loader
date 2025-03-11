package com.github.yewyc;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.HdrHistogram.Histogram;
import org.jboss.logging.Logger;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.ArrayList;
import java.util.List;

public class Task {

    private static final Logger LOGGER = Logger.getLogger(Task.class);

    private final String name;
    private final Runnable action;
    private final boolean trackData;
    private Histogram latencyHistogram;

    private List<Long> data = new ArrayList<>();

    public Task(String name, Runnable action) {
        this(name, action, false);
    }

    public Task(String name, Runnable action, boolean trackData) {
        this.name = name;
        this.action = action;
        this.trackData = trackData;
    }

    public String getName() {
        return name;
    }

    public void run() {
        this.action.run();
    }

    public void configure(long timeNs) {
        this.latencyHistogram = new Histogram(timeNs, 2);
    }

    public void recordValue(long value) {
        this.latencyHistogram.recordValue(value);
        if (this.trackData) {
            this.data.add(value);
        }
    }

    public void report(long intervalNs) {
        System.out.println("Task: " + this.getName());
        System.out.println("\t======= Latency =======");
        System.out.println("\tOperations: " + latencyHistogram.getTotalCount());
        System.out.println("\tAverageLatency(ns): " + latencyHistogram.getMean());
        System.out.println("\tMinLatency(ns): " + latencyHistogram.getMinValue());
        System.out.println("\tMaxLatency(ns): " + latencyHistogram.getMaxValue());
        System.out.println("\t90thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(90) / 1000000.0);
        System.out.println("\t95thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(95) / 1000000.0);
        System.out.println("\t99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99) / 1000000.0);
        System.out.println("\t99.9thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.9) / 1000000.0);
        System.out.println("\t99.99thPercentileLatency(ms): " + latencyHistogram.getValueAtPercentile(99.99) / 1000000.0);
        System.out.println("\t---");
        System.out.println("\tYou can " + ((latencyHistogram.getMean() < intervalNs) ? "increase" : "decrease") + " the opsPerSec by a factor of: " + String.format("%.2f", intervalNs / latencyHistogram.getMean()));
        System.out.println("\t---");
    }

    public void plot() {
        if (!this.trackData) {
            LOGGER.warn("Cannot plot because trackData is disabled. Set trackData to true in order to plot the data.");
            return;
        }
        LongArrayList operations = new LongArrayList();
        LongArrayList latency = new LongArrayList();
        for (int i = 0; i < this.data.size(); i++) {
            operations.add(i + 1);
            latency.add(this.data.get(i));
        }

        Table robberies =
                Table.create("Latency")
                        .addColumns(
                                LongColumn.create("Operation", operations.longStream()),
                                LongColumn.create("Latency ns", latency.longStream()));

        NumericColumn<?> x = robberies.nCol("Operation");
        NumericColumn<?> y = robberies.nCol("Latency ns");

        Layout layout = Layout.builder().title("Latency - " + this.getName()).build();
        ScatterTrace trace = ScatterTrace.builder(x, y).mode(ScatterTrace.Mode.LINE).build();
        Plot.show(new Figure(layout, trace));
    }
}
