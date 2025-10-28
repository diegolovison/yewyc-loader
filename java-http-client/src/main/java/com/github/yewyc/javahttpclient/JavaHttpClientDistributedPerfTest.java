package com.github.yewyc.javahttpclient;

import com.github.yewyc.DistributedBenchmark;
import com.github.yewyc.Benchmark;
import com.github.yewyc.WeightTask;

public class JavaHttpClientDistributedPerfTest extends JavaHttpClientPerfComplexTest {

    public static void main(String[] args) {
        try (Benchmark measure = new DistributedBenchmark(10, 10, 1, 5, 2)) {
            measure
                    .addTask(new WeightTask(task1(), 0.5), new WeightTask(task2(), 0.5))
                    .start()
                    .generateReport()
                    .plot();
        }
    }
}
