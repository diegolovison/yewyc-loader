package com.github.yewyc;

import tech.tablesaw.plotly.traces.ScatterTrace;

public class PlotData {
    double[] xData;
    double[] yData;
    ScatterTrace trace;

    public PlotData(double[] xData, double[] yData, ScatterTrace trace) {
        this.xData = xData;
        this.yData = yData;
        this.trace = trace;
    }
}
