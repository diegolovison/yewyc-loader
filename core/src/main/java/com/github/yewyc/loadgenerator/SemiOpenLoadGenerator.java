package com.github.yewyc.loadgenerator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Closed Model - wrk2 - Response-driven with intended time
 */
public class SemiOpenLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;

    public SemiOpenLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
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
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        super.channelRead0(ctx, msg);
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }
}
