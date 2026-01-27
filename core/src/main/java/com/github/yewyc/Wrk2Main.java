package com.github.yewyc;

import com.github.yewyc.stats.RateStatistics;
import com.github.yewyc.stats.Statistics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.github.yewyc.stats.Statistics.scale;

// -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=maxsize=1g,dumponexit=true,filename=data.jfr,settings=default
public class Wrk2Main {

    public static void main(String[] args) throws InterruptedException {

        // --timeout 2s --threads 2 --connections 10 --duration 30s --rate 100000 http://localhost:8080/hello
        if (args.length == 0) {
            System.err.println("Usage: WrkMain --timeout <timeout> --threads <threads> --connections <connections> --duration <duration> --rate <rate> http://localhost:8080/");
            return;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            params.put(args[i].substring(2), args[i + 1]);
        }

        int timeout = Integer.parseInt(params.get("timeout").replace("s", ""));
        int threads = Integer.parseInt(params.get("threads"));
        int connections = Integer.parseInt(params.get("connections"));;
        int duration = Integer.parseInt(params.get("duration").replace("s", ""));
        int rate = params.containsKey("rate") ? Integer.parseInt(params.get("rate")) : 0;
        String url = args[args.length - 1];

        Duration connectTimeout = Duration.ofSeconds(timeout);
        BenchmarkBuilder builder = new BenchmarkBuilder()
                .duration(duration).connections(connections).threads(threads).rate(rate)
                .urlBase(url)
                .warmUpDuration(6);
        try (Benchmark benchmark =  builder.build()) {
            benchmark
                .addTask(
                        new WeightTask(task(url, connectTimeout, connections))
                )
                .start()
                .generateReport(new WrkStats(threads, connections, url))
                .plot();
        }
    }

    private static Callable<Task> task(String url, Duration connectTimeout, int maxConnections) {

        class LocalTask extends Task {

            private HttpClient client;
            private HttpRequest request;
            private HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("test");
            }

            @Override
            public void initialize(ExecutorService executor) {
                this.client = HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .executor(executor)
                        .build();

                this.request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                this.handler = HttpResponse.BodyHandlers.ofString();
            }

            @Override
            public CompletableFuture<TaskStatus> submit() {
                CompletableFuture<HttpResponse<String>> responseAsync = client.sendAsync(request, handler);
                return responseAsync
                    .handle((response, ex) -> {
                        final TaskStatus localStatus;
                        // success
                        if (ex == null && response.statusCode() == 200) {
                            localStatus = TaskStatus.SUCCESS;
                        } else {
                            localStatus = TaskStatus.FAILED;
                        }
                        return localStatus;
                    });
            }

            @Override
            public void close() {
                this.client.shutdownNow();
                this.client.close();
            }
        }

        return LocalTask::new;
    }

    private static class WrkStats implements Consumer<Statistics> {

        private int threads;
        private int connections;
        private String url;

        public WrkStats(int threads, int connections, String url) {
            this.threads = threads;
            this.connections = connections;
            this.url = url;
        }

        @Override
        public void accept(Statistics stats) {
            // 1. Calculate Stats Objects
            RateStatistics throughput = RateStatistics.fromThroughput(stats.getTotalRequests());
            RateStatistics latency = RateStatistics.fromLatency(stats.getHistogram());

            // 2. Prepare Duration
            double duration = Math.max(0.001, (double) stats.duration());

            // 3. Print Header
            System.out.println("Running " + String.format("%8.2f", duration) + "s " + stats.getName() + " @ " + url);
            System.out.println("  " + threads + " threads and " + connections + " connections");
            System.out.println("  Thread Stats   Avg      Stdev     Max   +/- Stdev");

            System.out.println("    Latency   " +
                    String.format("%8.2f  ", latency.mean / scale) +
                    String.format("%8.2f  ", latency.stdDev / scale) +
                    String.format("%8.2f  ", latency.max / scale) + "   " +
                    String.format("%8.2f", latency.pctWithinStdev) + "%"
            );

            // 5. Print Throughput Row
            // Note: Divide throughput stats by 'threads' to get per-thread view
            System.out.println("    Req/Sec   " +
                    String.format("%8.2f  ", throughput.mean / threads) +
                    String.format("%8.2f  ", throughput.stdDev / threads) +
                    String.format("%8.2f  ", throughput.max / threads) + "     " +
                    String.format("%8.2f", throughput.pctWithinStdev) + "%"
            );

            // 6. Print Footer
            System.out.println("  " + throughput.totalSum + " requests in " + String.format("%.2f", duration) + "s, __MB read");
            System.out.println("Requests/sec: " + String.format("%8.2f", throughput.totalSum / duration));
            System.out.println("Transfer/sec:  __MB");
            System.out.println("Errors: " + stats.getTotalErrors());
        }
    }
}
