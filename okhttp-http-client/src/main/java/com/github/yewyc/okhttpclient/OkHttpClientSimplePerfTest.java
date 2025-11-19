package com.github.yewyc.okhttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

public class OkHttpClientSimplePerfTest extends OkHttpClientTask {

    public static void main(String[] args) {
        try (Benchmark benchmark = new Benchmark(60, 10, 1000)) {
            benchmark
                    .addTask(new WeightTask(createTask1(), 1.0))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
