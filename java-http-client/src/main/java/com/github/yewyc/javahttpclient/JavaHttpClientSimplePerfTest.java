package com.github.yewyc.javahttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

import java.time.Duration;

public class JavaHttpClientSimplePerfTest extends JavaHttpClientTask {

    public static void main(String[] args) {
        Duration connectTimeout = Duration.ofSeconds(5);
        try (Benchmark benchmark = new Benchmark(60, 100, 1, 60)) {
            benchmark
                    .addTask(new WeightTask(task1(connectTimeout), 1.0))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
