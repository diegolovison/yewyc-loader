package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

/**
 * Waits for the current request to complete, but inserts a delay if the response was too fast
 * to ensure the rate does not exceed the target interval.
 */
public class RateLimitedBlockingLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;

    public RateLimitedBlockingLoadGenerator(Channel channel, Scenario scenario, long intervalNs) {
        super(channel, scenario);
        this.intervalNs = intervalNs;
    }

    @Override
    protected void scheduleNextRequest() {
        long intendedTime = start + (this.id * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::scheduleNextRequestIfRunning, delayNs, TimeUnit.NANOSECONDS);
        } else {
            executeRequest(System.nanoTime(), intendedTime);
        }
    }

    @Override
    protected void onScenarioComplete() {
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }
}
