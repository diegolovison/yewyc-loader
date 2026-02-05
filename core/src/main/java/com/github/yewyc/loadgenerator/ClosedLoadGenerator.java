package com.github.yewyc.loadgenerator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.net.URL;

/**
 * Closed Model - wrk - waits for response before next request
 */
public class ClosedLoadGenerator extends AbstractLoadGenerator {

    public ClosedLoadGenerator(URL urlBase, Channel channel) {
        super(urlBase, channel);
    }

    @Override
    protected void scheduleNextRequest() {
        long now = System.nanoTime();
        executeRequest(now, now);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        super.channelRead0(ctx, msg);
        eventLoop.execute(this::scheduleNextRequestIfRunning);
    }
}
