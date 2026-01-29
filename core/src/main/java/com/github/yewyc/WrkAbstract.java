package com.github.yewyc;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.benchmark.BenchmarkBuilder;
import com.github.yewyc.stats.RateStatistics;
import com.github.yewyc.stats.Statistics;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.yewyc.stats.Statistics.scale;

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

        BenchmarkBuilder builder = new BenchmarkBuilder()
                .duration(duration).connections(connections).threads(threads).rate(rate)
                .urlBase(url).timeout(timeout).warmUpDuration(6);
        try (Benchmark benchmark =  builder.build()) {
            validate(benchmark);
            benchmark
                    .start()
                    .generateReport(new WrkAbstract.WrkStats(threads, connections, url))
                    .plot();
        }
    }

    protected abstract void validate(Benchmark benchmark);

    protected static class WrkStats implements Consumer<Statistics> {

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
            RateStatistics throughput = stats.getThroughput();
            RateStatistics latency = stats.getLatency();

            double duration = stats.duration();

            System.out.println("Running " + String.format("%8.2f", duration) + "s " + stats.getName() + " @ " + url);
            System.out.println("  " + threads + " threads and " + connections + " connections");
            System.out.println("  Thread Stats   Avg      Stdev     Max   +/- Stdev");

            // latency
            System.out.println("    Latency   " +
                    String.format("%8.2f  ", latency.mean / scale) +
                    String.format("%8.2f  ", latency.stdDev / scale) +
                    String.format("%8.2f  ", latency.max / scale) + "   " +
                    String.format("%8.2f", latency.pctWithinStdev) + "%"
            );

            // Note: Divide throughput stats by 'threads' to get per-thread view
            System.out.println("    Req/Sec   " +
                    String.format("%8.2f  ", throughput.mean / threads) +
                    String.format("%8.2f  ", throughput.stdDev / threads) +
                    String.format("%8.2f  ", throughput.max / threads) + "     " +
                    String.format("%8.2f", throughput.pctWithinStdev) + "%"
            );

            System.out.println("  " + throughput.totalSum + " requests in " + String.format("%.2f", duration) + "s, __MB read");
            System.out.println("Requests/sec: " + String.format("%8.2f", throughput.totalSum / duration));
            System.out.println("Transfer/sec:  __MB");
            System.out.println("Errors: " + stats.getTotalErrors());

            System.out.println("-----");

            double[][] xy = stats.getXY();
            double[] x = xy[0];
            double[] y = xy[1];
            double[] counter = xy[2];

            for (int i = 0; i < x.length; i++) {
                System.out.println((int) x[i] + " (" + (int) counter[i] + ")=" + String.format("%14.2f ", y[i]) + "ms");
            }

            System.out.println("-----");
        }
    }
}
