package com.github.yewyc.javahttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

import java.time.Duration;

public class JavaHttpClientPerfComplexTest extends JavaHttpClientSimplePerfTest {

    public static void main(String[] args) {
        Duration connectTimeout = Duration.ofSeconds(5);
        int maxConnections = 10;
        try (Benchmark benchmark = new Benchmark(10, 100, 1, 5)) {
            benchmark
                    .addTask(
                            new WeightTask(task1(connectTimeout, maxConnections), 0.5),
                            new WeightTask(task2(connectTimeout, maxConnections), 0.5)
                    )
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
