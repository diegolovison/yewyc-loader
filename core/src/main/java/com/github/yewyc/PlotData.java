package com.github.yewyc;

import tech.tablesaw.plotly.traces.ScatterTrace;

public class PlotData {
    Double[] xData;
    Double[] yData;
    ScatterTrace trace;

    public PlotData(Double[] xData, Double[] yData, ScatterTrace trace) {
        this.xData = xData;
        this.yData = yData;
        this.trace = trace;
    }
}
