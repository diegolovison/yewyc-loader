package com.github.yewyc;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.benchmark.BenchmarkRecord;
import com.github.yewyc.loadgenerator.LoadStrategy;
import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.scenario.ScenarioFactory;
import com.github.yewyc.sequence.Sequence;
import com.github.yewyc.step.RequestStep;
import com.github.yewyc.wrk.WrkStats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
            Scenario scenario = new Scenario(loop);
            List<Sequence> sequences = new ArrayList<>();
            sequences.add(new Sequence("seq-1", 0.5, List.of(new RequestStep("req-1-1", url, timeout), new RequestStep("req-1-2", url, timeout))));
            sequences.add(new Sequence("seq-2", 0.5, List.of(new RequestStep("req-2-1", url, timeout), new RequestStep("req-2-2", url, timeout))));
            scenario.setSequences(sequences);
            return scenario;
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
