package com.github.yewyc.javahttpclient;

import com.github.yewyc.DistributedMeasureLatency;
import com.github.yewyc.MeasureLatency;
import com.github.yewyc.MeasureLatencyType;

public class JavaHttpClientDistributedPerfTest extends JavaHttpClientPerfComplexTest {

    public static void main(String[] args) {
        JavaHttpClient client = new JavaHttpClient();
        try (MeasureLatency measure = new DistributedMeasureLatency(10, 10, 1, 5, MeasureLatencyType.GLOBAL, 2)
                .addTask(createTask1(client), createTask2(client)).start()) {
            measure.generateReport().plot();
        }
    }
}
