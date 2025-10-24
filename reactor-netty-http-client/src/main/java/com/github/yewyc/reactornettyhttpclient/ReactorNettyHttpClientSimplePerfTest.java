package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.MeasureLatency;

public class ReactorNettyHttpClientSimplePerfTest extends ReactorNettyHttpClientTask {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)) {
            measure
                    .addTask(createTask1())
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
