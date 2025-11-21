package com.github.yewyc;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.HistogramIterationValue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import static com.github.yewyc.Statistics.NANO_PER_MS;

public class WrkMain {

    static void main(String[] args) {

        // --timeout 2s --threads 2 --connections 10 --duration 30s --rate 100000"
        if (args.length == 0 || (args.length - 1) % 2 != 0) {
            System.err.println("Usage: WrkMain --timeout <timeout> --threads <threads> --connections <connections> --duration <duration> --rate <rate>");
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
        int rate = params.containsKey("rate") ? Integer.parseInt(params.get("rate")) : Model.CLOSED_MODEL.value;
        String url = args[args.length - 1];

        Duration connectTimeout = Duration.ofSeconds(timeout);
        try (Benchmark benchmark = new Benchmark(duration, threads, rate)) {
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

        class LocalTask extends HttpTask {

            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("test", connectTimeout, maxConnections);

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
        }

        return LocalTask::new;
    }

    private static abstract class HttpTask extends Task {

        HttpClient client;
        ExecutorService executor;

        HttpTask(String name, Duration connectTimeout, int maxConnections) {
            super(name);
            ThreadFactory threadFactory = r -> {
                Thread t = new Thread(r);
                t.setName("HttpClient-Worker-" + t.getId());
                return t;
            };
            executor = Executors.newFixedThreadPool(maxConnections, threadFactory);

            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .executor(executor)
                    .build();
        }

        @Override
        public void close() {
            this.executor.shutdownNow();
            this.executor.close();
            this.client.shutdownNow();
            this.client.close();
        }
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
            DoubleSummaryStatistics requestsStats = stats.getTotalRequests().stream().mapToDouble(s -> s).summaryStatistics();

            String requestPerSecondAverage =  String.format("%8.2f  ", requestsStats.getAverage());
            double requestsStdDev = Math.sqrt(
                    stats.getTotalRequests().stream().mapToDouble(s -> Math.pow(s - requestsStats.getAverage(), 2)).sum() / stats.getTotalRequests().size());
            String requestPerSecondStdDev =  String.format("%8.2f  ", requestsStdDev);
            String requestMax = String.format("%8.2f  ", requestsStats.getMax());
            String requestWithinStdev = String.format("%8.2f", statsWithinStdevRequests(requestsStats, requestsStdDev,
                    stats.getTotalRequests().stream().mapToLong(s -> s), stats.getTotalRequests().size()));

            System.out.println("Running " + stats.duration() + "s " + stats.getName() + " @ " + url);
            System.out.println("  " + threads + " threads and " + connections +" connections");
            System.out.println("  Thread Stats   Avg      Stdev     Max   +/- Stdev");
            System.out.println("    Latency   " + getLatencyAverage(stats) + "  " + getLatencyStdev(stats) + "   " + getLatencyMax(stats)  + "   " + statsWithinStdevLatency(stats) + "%");
            System.out.println("    Req/Sec   " + requestPerSecondAverage + "   " + requestPerSecondStdDev + "  " + requestMax + "     " + requestWithinStdev + "%");
            System.out.println("  " + getTotalRequests(stats) + " requests in " + stats.duration() + "s, __MB read");
            System.out.println("Requests/sec: " + getRequestsPerSecond(stats));
            System.out.println("Transfer/sec:  __MB");
        }

        private String getLatencyAverage(Statistics stats) {
            double value = stats.getHistogram().getMean() / NANO_PER_MS;
            return String.format("%8.2fms", value);
        }

        private String getLatencyStdev(Statistics stats) {
            double value = stats.getHistogram().getStdDeviation() / NANO_PER_MS;
            return String.format("%8.2fms", value);
        }

        private String getLatencyMax(Statistics stats) {
            double value = (double) stats.getHistogram().getMaxValue() / NANO_PER_MS;
            return String.format("%8.2fms", value);
        }

        private long getTotalRequests(Statistics stats) {
            return stats.getTotalRequests().stream().mapToLong(Long::longValue).sum();
        }

        private String getRequestsPerSecond(Statistics stats) {
            double value = (double) getTotalRequests(stats) / stats.duration();
            return String.format("%8.2f", value);
        }

        private String statsWithinStdevLatency(Statistics stats) {
            AbstractHistogram histogram = stats.getHistogram();
            double stdDev = histogram.getStdDeviation();
            double lower = histogram.getMean() - stdDev;
            double upper = histogram.getMean() + stdDev;
            long sum = 0;
            for (var it = histogram.allValues().iterator(); it.hasNext();) {
                HistogramIterationValue value = it.next();
                if (value.getValueIteratedFrom() >= lower && value.getValueIteratedTo() <= upper) {
                    sum += value.getCountAddedInThisIterationStep();
                }
            }
            double value = 100d * sum / getTotalRequests(stats);
            return String.format("%8.2f", value);
        }

        private double statsWithinStdevRequests(DoubleSummaryStatistics stats, double stdDev, LongStream stream, int count) {
            double lower = stats.getAverage() - stdDev;
            double upper = stats.getAverage() + stdDev;
            return 100d * stream.filter(reqs -> reqs >= lower && reqs <= upper).count() / count;
        }
    }
}
