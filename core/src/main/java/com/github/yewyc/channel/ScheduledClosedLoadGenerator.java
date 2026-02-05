package com.github.yewyc.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.net.URL;

/**
 * Closed Model - wrk2 - Response-driven with intended time
 */
public class ScheduledClosedLoadGenerator extends AbstractLoadGenerator {

    private final long intervalNs;
    private int i = 0;

    public ScheduledClosedLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
        this.intervalNs = intervalNs;
    }

    @Override
    protected void scheduleNextRequest() {
        long intendedTime = start + (i * this.intervalNs);
        boolean ok = executeRequest(intendedTime);
        if (ok) {
            i++;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        super.channelRead0(ctx, msg);
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }

    protected void reset() {
        this.i = 0;
        this.start = nan;
    }
}
