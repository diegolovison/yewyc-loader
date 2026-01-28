package com.github.yewyc.stats;

import com.github.yewyc.PlotData;
import tech.tablesaw.plotly.traces.ScatterTrace;

import static tech.tablesaw.plotly.traces.ScatterTrace.Fill.TO_ZERO_Y;

public class StatisticsPlot {

    public static PlotData plot(Statistics stats, int chartIndex) {
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
