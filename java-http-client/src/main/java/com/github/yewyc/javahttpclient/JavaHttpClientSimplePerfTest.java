package com.github.yewyc.javahttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

import java.time.Duration;

public class JavaHttpClientSimplePerfTest extends JavaHttpClientTask {

    public static void main(String[] args) {
        Duration connectTimeout = Duration.ofSeconds(5);
        int maxConnections = 10;
        try (Benchmark benchmark = new Benchmark(60, 10, 1000)) {
            benchmark
                    .addTask(
                            new WeightTask(task1(connectTimeout, maxConnections), 1.0)
                    )
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
