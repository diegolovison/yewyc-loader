package com.github.yewyc.step;

import com.github.yewyc.scenario.ScenarioContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class RequestStep extends Step {

    private final boolean assertResponseOperation = System.getProperty("assertResponseOperation", "false").equals("true");

    private final Bootstrap bootstrap;
    private final String host;
    private final int port;
    private final DefaultFullHttpRequest sharedRequest;

    private Channel activeChannel;
    private CompletableFuture<Void> currentFuture;

    private final Duration timeout;

    public RequestStep(String url, Duration timeout) {
        this(null, url, timeout);
    }
    public RequestStep(String name, String url, Duration timeout) {
        super(name);
        this.timeout = timeout;

        URI uri = URI.create(url);
        this.host = uri.getHost();
        this.port = uri.getPort() == -1 ? (uri.getScheme().equals("https") ? 443 : 80) : uri.getPort();

        // 1. Setup Bootstrap once
        this.bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
        this.sharedRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath(), Unpooled.EMPTY_BUFFER);
        this.sharedRequest.headers().set(HttpHeaderNames.HOST, host);
        this.sharedRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        this.sharedRequest.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    }

    @Override
    public CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id) {
        assert loop.inEventLoop();
        this.currentFuture = new CompletableFuture<>();
        sendRequest(activeChannel, id);
        return currentFuture;
    }

    private void sendRequest(Channel ch, long id) {
        FullHttpRequest localRequest = sharedRequest.retainedDuplicate();
        if (this.assertResponseOperation) {
            localRequest.headers().set("X-Request-Id", id);
        }
        ch.writeAndFlush(localRequest);
    }

    public CompletableFuture<Void> setupClient(EventLoop loop, ExecutorService workerPool) {
        assert loop.inEventLoop();
        // Submit to worker pool
        CompletableFuture<CompletableFuture<Void>> nestedFuture =
                CompletableFuture.supplyAsync(() -> setupClient(loop), workerPool);
        return nestedFuture.thenCompose(future -> future);
    }

    private CompletableFuture<Void> setupClient(EventLoop loop) {
        assert !loop.inEventLoop();
        CompletableFuture<Void> channelFuture = new CompletableFuture<>();

        loop.execute(() -> {
            bootstrap.clone()
                    .group(loop)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS));
                            ch.pipeline().addLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS));
                            ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(1024 * 1024));
                            ch.pipeline().addLast(new ResponseHandler());
                        }
                    })
                    .connect(host, port)
                    .addListener((ChannelFuture cf) -> {
                        if (cf.isSuccess()) {
                            this.activeChannel = cf.channel();
                            channelFuture.complete(null);
                        } else {
                            channelFuture.completeExceptionally(cf.cause());
                        }
                    });
        });
        return channelFuture;
    }

    private class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            assert currentFuture != null;
            if (msg.status().code() != 200) {
                currentFuture.completeExceptionally(new RuntimeException("Non-200 Status: " + msg.status()));
            } else {
                currentFuture.complete(null);
            }
            currentFuture = null;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (currentFuture != null) {
                currentFuture.completeExceptionally(cause);
                currentFuture = null;
            }
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (currentFuture != null) {
                currentFuture.completeExceptionally(new RuntimeException("Channel closed"));
                currentFuture = null;
            }
            super.channelInactive(ctx);
        }
    }
}
