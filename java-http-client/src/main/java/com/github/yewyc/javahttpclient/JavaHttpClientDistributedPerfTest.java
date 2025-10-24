package com.github.yewyc.javahttpclient;

import com.github.yewyc.DistributedMeasureLatency;
import com.github.yewyc.MeasureLatency;

public class JavaHttpClientDistributedPerfTest extends JavaHttpClientPerfComplexTest {

    public static void main(String[] args) {
        try (MeasureLatency measure = new DistributedMeasureLatency(10, 10, 1, 5, 2)) {
            measure
                    .addTask(createTask1(), createTask2())
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
