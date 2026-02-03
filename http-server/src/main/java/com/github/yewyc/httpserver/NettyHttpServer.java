package com.github.yewyc.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpServer {

    public static void main(String[] args) throws Exception {
        new NettyHttpServer().start(8080);
    }

    private static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(); 

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_REUSEADDR, true) 
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     allChannels.add(ch);
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new HttpServerCodec());
                     p.addLast(new HttpObjectAggregator(65536));
                     p.addLast(new DefaultHandler());
                 }
             });

            System.out.println("Netty Server started on port " + port);

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
            
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static class DefaultHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static final ByteBuf UNKNOWN_BUF = Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer("unknown", CharsetUtil.UTF_8));

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

            boolean keepAlive = HttpUtil.isKeepAlive(req);
            String requestId = req.headers().get("X-Request-Id");

            if ("/hello".equals(req.uri())) {
                writeReponse(ctx, requestId, keepAlive);
            } else if ("/hello2".equals(req.uri())) {
                ctx.executor().schedule(() -> {
                    writeReponse(ctx, requestId, keepAlive);
                }, 100, TimeUnit.MICROSECONDS);
            } else if ("/fullGC".equals(req.uri())) {
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, OK,
                        Unpooled.copiedBuffer("fullGC...\n", CharsetUtil.UTF_8)
                );
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                response.headers().set(CONNECTION, CLOSE);
                ctx.writeAndFlush(response).addListener(f -> {
                    allChannels.close();
                    System.gc();
                });
            }
        }

        private void writeReponse(ChannelHandlerContext ctx, String requestId, boolean keepAlive) {
            ByteBuf content;
            if (requestId == null) {
                content = UNKNOWN_BUF.retainedDuplicate();
            } else {
                content = Unpooled.copiedBuffer(requestId, CharsetUtil.UTF_8);
            }

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
            response.headers().set(CONTENT_TYPE, TEXT_PLAIN);
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            if (keepAlive) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                response.headers().set(CONNECTION, CLOSE);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }
}
