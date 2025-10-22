package com.github.yewyc.okhttpclient;

import com.github.yewyc.MeasureLatency;

public class OkHttpClientSimplePerfTest extends OkHttpClientTask {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)
                .addTask(createTask1()).start()) {
            measure.generateReport().plot();
        }
    }
}
