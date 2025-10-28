package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

public class ReactorNettyHttpClientSimplePerfTest extends ReactorNettyHttpClientTask {

    public static void main(String[] args) {
        try (Benchmark benchmark = new Benchmark(60, 100, 1, 60)) {
            benchmark
                    .addTask(new WeightTask(createTask1(), 1.0))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
