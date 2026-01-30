package com.github.yewyc.plot;

import com.github.yewyc.stats.Statistic;
import com.github.yewyc.stats.StatisticPhase;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Config;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StatisticsPlot {

    public static void plot(List<StatisticPhase> statistics) {
        List<Trace> allTraces = new ArrayList<>();

        double globalXIndex = 1;

        for (int i = 0; i < statistics.size(); i++) {
            StatisticPhase statisticPhase = statistics.get(i);
            double[][] xy = statisticPhase.getXY();
            double[] latencyRaw = xy[1];
            double[] throughputRaw = xy[2];

            double[] xValues = new double[latencyRaw.length];
            double[] latencyValues = new double[latencyRaw.length];
            double[] throughputValues = new double[latencyRaw.length];

            for (int j = 0; j < latencyRaw.length; j++) {
                xValues[j] = globalXIndex;
                latencyValues[j] = latencyRaw[j];
                throughputValues[j] = throughputRaw[j];
                globalXIndex++;
            }

            allTraces.add(ScatterTrace.builder(xValues, latencyValues)
                    .mode(ScatterTrace.Mode.LINE)
                    .name(statisticPhase.getName() + " (Lat)")
                    .build());

            allTraces.add(ScatterTrace.builder(xValues, throughputValues)
                    .mode(ScatterTrace.Mode.LINE)
                    .xAxis("x2")
                    .yAxis("y2")
                    .name(statisticPhase.getName() + " (Thr)")
                    .build());
        }

        Grid grid = Grid.builder()
                .rows(2)
                .columns(1)
                .pattern(Grid.Pattern.INDEPENDENT)
                .build();

        Layout layout = Layout.builder()
                .title("Perf Report - " + new Date())
                .grid(grid)
                .autosize(true)
                .height(800)
                .yAxis(Axis.builder().title("Latency (ms)").build())
                .yAxis2(Axis.builder().title("Throughput (ops/sec)").build())
                .build();

        Config config = Config.builder()
                .responsive(true)
                .build();

        Figure figure = new Figure(layout, config, allTraces.toArray(new Trace[0]));
        Plot.show(figure, new File("/tmp/report-" + UUID.randomUUID() + ".html"));
    }
}

