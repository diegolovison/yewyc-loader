package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;

/**
 * Fires requests on a fixed schedule regardless of whether previous requests have completed.
 * Maintains a constant request rate (RPS) even if the system slows down.
 */
public class NonBlockingLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;

    public NonBlockingLoadGenerator(Channel channel, Scenario scenario, long intervalNs) {
        super(channel, scenario);
        this.intervalNs = intervalNs;
    }

    protected void scheduleNextRequest() {
        long intendedTime = start + (this.id * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::scheduleNextRequestIfRunning, delayNs, TimeUnit.NANOSECONDS);
        } else {
            executeRequest(System.nanoTime(), intendedTime);
            // TODO eventLoop.execute adds a small amount of overhead compared to a direct loop but prevent stack overflows from deep recursion
            eventLoop.execute(this::scheduleNextRequestIfRunning);
        }
    }

    @Override
    protected void onScenarioComplete() {

    }
}
