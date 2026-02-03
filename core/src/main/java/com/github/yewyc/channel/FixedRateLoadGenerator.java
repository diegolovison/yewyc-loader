package com.github.yewyc.channel;

import io.netty.channel.Channel;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Open Model
 */
public class FixedRateLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;
    private int i = 0;
    private long startIntendedTime;

    public FixedRateLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
        this.intervalNs = intervalNs;
    }

    protected void scheduleNextRequest() {
        if (this.startIntendedTime == nan) {
            this.startIntendedTime = System.nanoTime();
        }
        long intendedTime = startIntendedTime + (i * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::scheduleNextRequestIfRunning, delayNs, TimeUnit.NANOSECONDS);
        } else {
            boolean ok = executeRequest(intendedTime);
            if (ok) {
                i++;
            }
            // TODO eventLoop.execute adds a small amount of overhead compared to a direct loop but prevent stack overflows from deep recursion
            eventLoop.execute(this::scheduleNextRequestIfRunning);
        }
    }

    protected void reset() {
        this.i = 0;
        this.startIntendedTime = nan;
    }

}
