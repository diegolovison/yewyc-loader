package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;

public class JavaHttpClientSimplePerfTest extends JavaHttpClientTask {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)
                .addTask(createTask1()).start()) {
            measure.generateReport().plot();
        }
    }
}
