package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.MeasureLatencyType;

public class JavaHttpClientSimplePerfTest extends JavaHttpClientTask {

    public static void main(String[] args) {
        JavaHttpClient client = new JavaHttpClient();
        try (MeasureLatency measure = new MeasureLatency(10, 100, 1, 5, MeasureLatencyType.GLOBAL)
                .addTask(createTask1(client)).start()) {
            measure.generateReport().plot();
        }
    }
}
