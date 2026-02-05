package com.github.yewyc.channel;

import io.netty.channel.Channel;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Open Model - wrk3 - Fixed rate, fire requests at that rate
 */
public class OpenLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;

    public OpenLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
        this.intervalNs = intervalNs;
    }

    protected void scheduleNextRequest() {
        long intendedTime = start + (this.id * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::scheduleNextRequestIfRunning, delayNs, TimeUnit.NANOSECONDS);
        } else {
            executeRequest(intendedTime);
            // TODO eventLoop.execute adds a small amount of overhead compared to a direct loop but prevent stack overflows from deep recursion
            eventLoop.execute(this::scheduleNextRequestIfRunning);
        }
    }
}
