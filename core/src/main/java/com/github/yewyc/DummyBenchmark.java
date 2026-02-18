package com.github.yewyc;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.benchmark.BenchmarkRecord;
import com.github.yewyc.loadgenerator.LoadStrategy;
import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.scenario.ScenarioFactory;
import com.github.yewyc.sequence.Sequence;
import com.github.yewyc.step.RequestStep;
import com.github.yewyc.step.Step;
import com.github.yewyc.wrk.WrkStats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DummyBenchmark {

    public static void main(String[] args) throws Exception {
        DummyBenchmark benchmark = new DummyBenchmark();
        benchmark.exec(args);
    }

    protected void exec(String[] args) {
        Duration timeout = Duration.ofSeconds(10);
        int threads = 1;
        int connections = 1;
        Duration duration = Duration.ofSeconds(10);
        int rate = 100;
        String url = "http://localhost:8080";

        BenchmarkRecord benchmarkRecord = new BenchmarkRecord(threads, duration, rate, connections, Duration.ofSeconds(6), LoadStrategy.RATE_LIMIT);

        ScenarioFactory scenarioFactory = (loop) -> {
            try (ExecutorService executor = Executors.newFixedThreadPool(1)) {
                Scenario scenario = new Scenario(loop);
                List<Sequence> sequences = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    List<Step> steps = new ArrayList<>();
                    for (int j = 0; j < 2; j++) {
                        RequestStep step = new RequestStep("req-" + i + "-" + j, url, timeout);
                        step.setupClient(loop, executor);
                        steps.add(step);
                    }
                    sequences.add(new Sequence("seq-" + i, 0.5, steps));
                }
                scenario.setSequences(sequences);
                return scenario;
            }
        };

        try (Benchmark benchmark =  new Benchmark(benchmarkRecord)) {
            benchmark
                    .start(scenarioFactory)
                    .generateReport(new WrkStats(threads, connections, url))
                    .plot();
            benchmark.dump();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
