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

    public FixedRateLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
        this.intervalNs = intervalNs;
    }

    protected void send() {
        long intendedTime = startIntendedTime + (i * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::loopSend, delayNs, TimeUnit.NANOSECONDS);
        } else {
            sendRequest(intendedTime);
            i++;
            eventLoop.execute(this::loopSend);
        }
    }

    protected void reset() {
        super.reset();
        this.i = 0;
    }

}
