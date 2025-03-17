package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.MeasureLatencyType;

public class JavaHttpClientPerfComplexTest extends JavaHttpClientSimplePerfTest {

    public static void main(String[] args) {
        JavaHttpClient client = new JavaHttpClient();
        try (MeasureLatency measure = new MeasureLatency(10, 100, 1, 5, MeasureLatencyType.GLOBAL)
                .addTask(createTask1(client), createTask2(client)).start()) {
            measure.generateReport().plot();
        }
    }
}
