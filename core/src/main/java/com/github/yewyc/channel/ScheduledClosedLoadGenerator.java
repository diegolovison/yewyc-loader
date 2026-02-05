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

    public ScheduledClosedLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        super(urlBase, channel);
        this.intervalNs = intervalNs;
    }

    @Override
    protected void scheduleNextRequest() {
        long intendedTime = start + (this.id * this.intervalNs);
        executeRequest(intendedTime);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        super.channelRead0(ctx, msg);
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }
}
