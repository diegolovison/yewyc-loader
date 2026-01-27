package com.github.yewyc;

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

    public List<Statistics> run(int rate, int connections, int threads, String urlBaseParam, Duration duration, Duration warmUpDuration, Duration timeout) {

        URL urlBase;
        try {
            urlBase = URI.create(urlBaseParam).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        List<Statistics> tasks = new ArrayList<>();

        long intervalNs;
        if (rate == 0) {
            intervalNs = 0;
        } else {
            // each instance of StatsChannelInboundHandler is a new class
            intervalNs = 1000000000 / (rate / connections);
        }

        log.info("Benchmark initialization with " + (rate == 0 ? " a closed model" : "an open model"));

        EventLoopGroup group = new NioEventLoopGroup(threads);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ChannelPipeline p = ch.pipeline();

                            if (timeout != null) {
                                p.addLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS));
                                p.addLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS));
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
            for (int i = 0; i < connections; i++) {
                Channel channel = b.connect(urlBase.getHost(), urlBase.getPort()).sync().channel();
                channels.add(channel);
            }
            if (warmUpDuration != null) {
                tasks.add(runWarmupPhase(channels, "warm-up", warmUpDuration));
            }
            tasks.add(runTestPhase(channels, "test", duration));

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
