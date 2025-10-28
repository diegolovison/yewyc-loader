package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.WeightTask;

public class ReactorNettyHttpClientSimplePerfTest extends ReactorNettyHttpClientTask {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)) {
            measure
                    .addTask(new WeightTask(createTask1(), 1.0))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
