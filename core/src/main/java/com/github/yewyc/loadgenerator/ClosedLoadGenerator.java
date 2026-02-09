package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import io.netty.channel.Channel;

/**
 * Closed Model - wrk - waits for response before next request
 */
public class ClosedLoadGenerator extends AbstractLoadGenerator {

    public ClosedLoadGenerator(Channel channel, Scenario scenario) {
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
