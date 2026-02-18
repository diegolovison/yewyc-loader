package com.github.yewyc.wrk;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.benchmark.BenchmarkRecord;
import com.github.yewyc.loadgenerator.LoadStrategy;
import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.scenario.ScenarioFactory;
import com.github.yewyc.step.RequestStep;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class WrkAbstract {

    protected void exec(String[] args) {

        boolean detectBlockingOperation = System.getProperty("detectBlockingOperation", "false").equals("true");
        if (detectBlockingOperation) {
            reactor.blockhound.BlockHound.install();
        }

        System.out.println("Starting Wrk2Main: " + String.join(" ", args));

        // --timeout 2s --threads 2 --connections 10 --duration 30s --rate 100000 http://localhost:8080/hello
        if (args.length == 0) {
            System.err.println("Usage: WrkMain --timeout <timeout> --threads <threads> --connections <connections> --duration <duration> --rate <rate> http://localhost:8080/");
            return;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            params.put(args[i].substring(2), args[i + 1]);
        }

        Duration timeout = Duration.ofSeconds(Integer.parseInt(params.get("timeout").replace("s", "")));
        int threads = Integer.parseInt(params.get("threads"));
        int connections = Integer.parseInt(params.get("connections"));;
        int duration = Integer.parseInt(params.get("duration").replace("s", ""));
        int rate = params.containsKey("rate") ? Integer.parseInt(params.get("rate")) : 0;
        String url = args[args.length - 1];

        BenchmarkRecord benchmarkRecord = new BenchmarkRecord(threads, Duration.ofSeconds(duration), rate, connections, Duration.ofSeconds(6), getMode());

        ScenarioFactory scenarioFactory = (loop) -> {
            try (ExecutorService executor = Executors.newFixedThreadPool(1)) {
                RequestStep step = new RequestStep(url, timeout);
                step.setupClient(loop, executor);
                Scenario scenario = new Scenario(loop);
                scenario.addStep(step);
                return scenario;
            }
        };

        try (Benchmark benchmark =  new Benchmark(benchmarkRecord)) {
            benchmark
                    .start(scenarioFactory)
                    .generateReport(new WrkStats(threads, connections, url))
                    .plot();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract LoadStrategy getMode();
}
