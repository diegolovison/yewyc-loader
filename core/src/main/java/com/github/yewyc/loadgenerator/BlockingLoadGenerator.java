package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import io.netty.channel.Channel;

/**
 * Strictly waits for the current request to complete before scheduling the next one.
 * Throughput is fully dependent on system latency.
 */
public class BlockingLoadGenerator extends AbstractLoadGenerator {

    public BlockingLoadGenerator(Channel channel, Scenario scenario) {
        super(channel, scenario);
    }

    @Override
    protected void scheduleNextRequest() {
        long now = System.nanoTime();
        executeRequest(now, now);
    }

    @Override
    protected void onScenarioComplete() {
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }
}
