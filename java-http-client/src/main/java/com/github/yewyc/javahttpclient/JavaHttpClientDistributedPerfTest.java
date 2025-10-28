package com.github.yewyc.javahttpclient;

import com.github.yewyc.DistributedMeasureLatency;
import com.github.yewyc.MeasureLatency;
import com.github.yewyc.WeightTask;

public class JavaHttpClientDistributedPerfTest extends JavaHttpClientPerfComplexTest {

    public static void main(String[] args) {
        try (MeasureLatency measure = new DistributedMeasureLatency(10, 10, 1, 5, 2)) {
            measure
                    .addTask(new WeightTask(task1(), 0.5), new WeightTask(task2(), 0.5))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
