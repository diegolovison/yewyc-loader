package com.github.yewyc.plot;

import com.github.yewyc.stats.Statistics;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Grid;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class StatisticsPlot {

    public static void plot(List<Statistics> tasks) {
        List<Trace> traces = new ArrayList<>();
        // `i` is 1 because of https://github.com/jtablesaw/tablesaw/issues/1284
        int i = 1;
        for (Statistics stats : tasks) {
            PlotData plotData = StatisticsPlot.plot(stats, i);
            traces.add(plotData.trace);
            i += 1;
        }
        if (traces.size() > 0) {
            Grid grid = Grid.builder().columns(1).rows(traces.size()).pattern(Grid.Pattern.INDEPENDENT).build();
            Layout layout = Layout.builder().width(1700).height(800).title("Response time mean(ms)").grid(grid).build();
            Figure figure = new Figure(layout, traces.stream().toArray(Trace[]::new));
            Plot.show(figure, new File("/tmp/report-" + UUID.randomUUID() + ".html"));
        }
    }

    private static PlotData plot(Statistics stats, int chartIndex) {
        double[][] xy = stats.getXY();
        double[] xData = xy[0];
        double[] yData = xy[1];
        ScatterTrace trace = ScatterTrace.builder(xData, yData)
                .mode(ScatterTrace.Mode.LINE)
                .name(stats.getName())
                .xAxis("x" + chartIndex).yAxis("y" + chartIndex)
                .fill(TO_ZERO_Y)
                .build();
        return new PlotData(xData, yData, trace);
    }
}
