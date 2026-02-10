package com.github.yewyc.wrk;

import com.github.yewyc.stats.RateStatistics;
import com.github.yewyc.stats.StatisticPhase;
import com.github.yewyc.stats.StatisticTick;

import java.util.function.Consumer;

import static com.github.yewyc.stats.Statistic.scale;

public class WrkStats implements Consumer<StatisticPhase> {

    private int threads;
    private int connections;
    private String url;

    public WrkStats(int threads, int connections, String url) {
        this.threads = threads;
        this.connections = connections;
        this.url = url;
    }

    @Override
    public void accept(StatisticPhase statisticPhase) {
        RateStatistics throughput = statisticPhase.getThroughput();
        RateStatistics latency = statisticPhase.getLatency();

        double duration = statisticPhase.duration().toMillis() / 1_000.0;

        System.out.println("Running " + String.format("%8.2f", duration) + "s " + statisticPhase.getName() + " @ " + url);
        System.out.println("  " + threads + " threads and " + connections + " connections");
        System.out.println("  Thread Stats   Avg      Stdev     Max   +/- Stdev");

        // latency
        System.out.println("    Latency   " +
                String.format("%8.2f  ", latency.mean / scale) +
                String.format("%8.2f  ", latency.stdDev / scale) +
                String.format("%8.2f  ", latency.max / scale) + "   " +
                String.format("%8.2f", latency.pctWithinStdev) + "%"
        );

        System.out.println("    Req/Sec   " +
                String.format("%8.2f  ", throughput.mean) +
                String.format("%8.2f  ", throughput.stdDev) +
                String.format("%8.2f  ", throughput.max) + "     " +
                String.format("%8.2f", throughput.pctWithinStdev) + "%"
        );

        System.out.println("  " + throughput.totalSum + " requests in " + String.format("%.2f", duration) + "s, __MB read");
        System.out.println("Requests/sec: " + String.format("%8.2f", throughput.totalSum / duration));
        System.out.println("Transfer/sec:  __MB");

        System.out.println("-----");
        for (int i = 0; i < statisticPhase.getStatisticTicks().size(); i++) {
            StatisticTick tick = statisticPhase.getStatisticTicks().get(i);
            System.out.println((i + 1) + " (" + tick.counter() + ")=" + String.format("%14.2f ", tick.latency().getMean() / scale) + "ms");
        }
        System.out.println("-----");
    }
}
