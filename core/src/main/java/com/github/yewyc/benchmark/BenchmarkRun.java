package com.github.yewyc.benchmark;

import com.github.yewyc.channel.RunChannelInboundHandler;
import com.github.yewyc.stats.Statistics;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BenchmarkRun {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRun.class);

    public List<Statistics> run(BenchmarkRecord record) {

        URL urlBase;
        try {
            urlBase = URI.create(record.urlBase()).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        List<Statistics> tasks = new ArrayList<>();

        long intervalNs;
        if (record.isClosedModel()) {
            intervalNs = 0;
            log.info("Benchmark initialization with a closed model");
        } else {
            double requestsPerSecondPerConnection = record.requestsPerSecondPerConnection();
            intervalNs = (long) (Duration.ofSeconds(1).toNanos() / requestsPerSecondPerConnection);
            log.info("Benchmark initialization with an open model. requests_per_second_per_connection=" + requestsPerSecondPerConnection + ", max_requests_warmup_phase=" + (record.expectedWarmUpRequests()) + ", max_requests_test_phase=" + (record.expectedTestRequests()));
        }

        EventLoopGroup group = new NioEventLoopGroup(record.threads());
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ChannelPipeline p = ch.pipeline();

                            if (record.hasTimeout()) {
                                p.addLast(new ReadTimeoutHandler(record.timeout().toSeconds(), TimeUnit.SECONDS));
                                p.addLast(new WriteTimeoutHandler(record.timeout().toSeconds(), TimeUnit.SECONDS));
                            }

                            // Monitor open and close connections
                            p.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    log.info("New connection opened: " + ctx.channel().remoteAddress());
                                    super.channelActive(ctx);
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    log.info("Connection closed: " + ctx.channel().remoteAddress());
                                    super.channelInactive(ctx);
                                }
                            });

                            p.addLast(new HttpClientCodec());
                            p.addLast(new HttpObjectAggregator(1024 * 1024));

                            // Monitor In-flight HTTP requests that haven't completed yet during shutdown
                            p.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    if (cause instanceof io.netty.handler.codec.PrematureChannelClosureException) {
                                        log.info("Channel closed during shutdown: " + cause.getMessage());
                                    } else {
                                        log.error("Unexpected exception in channel", cause);
                                    }
                                    ctx.close();
                                }
                            });

                            p.addLast("run-handler", new RunChannelInboundHandler(urlBase, ch.read(), intervalNs));
                        }
                    });

            List<Channel> channels = new ArrayList<>();
            for (int i = 0; i < record.connections(); i++) {
                Channel channel = b.connect(urlBase.getHost(), urlBase.getPort()).sync().channel();
                channels.add(channel);
            }
            if (record.hasWarmUp()) {
                tasks.add(runWarmupPhase(channels, "warm-up", record.warmUpDuration()));
            }
            tasks.add(runTestPhase(channels, "test", record.duration()));

            group.shutdownGracefully();
            group.terminationFuture().sync();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Benchmark finished");

        return tasks;
    }

    private Statistics runWarmupPhase(List<Channel> channels, String name, Duration duration) throws InterruptedException {
        return this.runPhase(channels, name, duration);
    }

    private Statistics runTestPhase(List<Channel> channels, String name, Duration duration) throws InterruptedException {
        return this.runPhase(channels, name, duration);
    }

    /*
     * block operation
     */
    private Statistics runPhase(List<Channel> channels, String name, Duration duration) throws InterruptedException {
        List<RunChannelInboundHandler> listeners = new ArrayList<>();
        channels.forEach(ch -> {
            RunChannelInboundHandler handler = (RunChannelInboundHandler) ch.pipeline().get("run-handler");
            listeners.add(handler);
        });
        log.info("Starting the phase: " + name);
        listeners.forEach(h -> h.start(name));
        Thread.sleep(duration.toMillis());
        listeners.forEach(RunChannelInboundHandler::stop);

        List<Statistics> stats = new ArrayList<>();
        for (RunChannelInboundHandler listener : listeners) {
            stats.add(listener.collectStatistics());
        }
        Statistics stat = stats.getFirst();
        for (int i = 1; i < stats.size(); i++) {
            stat.merge(stats.get(i));
        }
        log.info("Finished the phase: " + name);
        return stat;
    }
}
