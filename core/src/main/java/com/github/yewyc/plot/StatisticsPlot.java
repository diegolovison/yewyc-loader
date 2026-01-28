package com.github.yewyc.plot;

import com.github.yewyc.stats.Statistics;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatisticsPlot {

    public static void plot(List<Statistics> tasks) {
        List<Trace> traces = new ArrayList<>();

        double globalXIndex = 1;

        for (int i = 0; i < tasks.size(); i++) {
            Statistics stats = tasks.get(i);
            double[][] xy = stats.getXY();
            double[] yValuesRaw = xy[1]; // Get Y values

            double[] xValues = new double[yValuesRaw.length];
            double[] yValues = new double[yValuesRaw.length];

            for (int j = 0; j < yValuesRaw.length; j++) {
                xValues[j] = globalXIndex;
                yValues[j] = yValuesRaw[j];
                globalXIndex++;
            }

            ScatterTrace trace = ScatterTrace.builder(xValues, yValues)
                    .mode(ScatterTrace.Mode.LINE)
                    .name(stats.getName())
                    .build();

            traces.add(trace);
        }

        Layout layout = Layout.builder().title("Response Time Mean (ms)").build();

        Figure figure = new Figure(layout, traces.toArray(new Trace[0]));
        Plot.show(figure, new File("/tmp/report-" + UUID.randomUUID() + ".html"));
    }
}
