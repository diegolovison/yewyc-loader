package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

/**
 * Closed Model - wrk2 - Response-driven with intended time
 */
public class SemiOpenLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;

    public SemiOpenLoadGenerator(Channel channel, Scenario scenario, long intervalNs) {
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
