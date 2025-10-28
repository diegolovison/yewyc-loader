package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.WeightTask;

public class JavaHttpClientSimplePerfTest extends JavaHttpClientTask {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)) {
            measure
                    .addTask(new WeightTask(task1(), 1.0))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
