package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.WeightTask;

public class JavaHttpClientPerfComplexTest extends JavaHttpClientSimplePerfTest {

    public static void main(String[] args) {
        try (MeasureLatency measure = new MeasureLatency(10, 100, 1, 5)) {
            measure
                    .addTask(new WeightTask(createTask1(), 0.5), new WeightTask(createTask2(), 0.5))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
