package com.github.yewyc.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import java.net.URL;

/**
 * Closed model
 */
public class LoadGenerator extends AbstractLoadGenerator {

    public LoadGenerator(URL urlBase, Channel channel) {
        super(urlBase, channel);
    }

    @Override
    protected void send() {
        sendRequest(System.nanoTime());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        super.channelRead0(ctx, msg);
        eventLoop.execute(this::send);
    }
}
