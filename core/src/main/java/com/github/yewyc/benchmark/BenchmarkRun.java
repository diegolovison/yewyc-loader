package com.github.yewyc.benchmark;

import com.github.yewyc.loadgenerator.AbstractLoadGenerator;
import com.github.yewyc.loadgenerator.BlockingLoadGenerator;
import com.github.yewyc.loadgenerator.LoadStrategy;
import com.github.yewyc.loadgenerator.NonBlockingLoadGenerator;
import com.github.yewyc.loadgenerator.RateLimitedBlockingLoadGenerator;
import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.scenario.ScenarioFactory;
import com.github.yewyc.stats.Statistic;
import com.github.yewyc.stats.StatisticConverter;
import com.github.yewyc.stats.StatisticPhase;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkRun {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRun.class);

    private final ScenarioFactory scenarioFactory;

    public BenchmarkRun(ScenarioFactory scenarioFactory) {
        this.scenarioFactory = scenarioFactory;
    }

    public List<StatisticPhase> run(BenchmarkRecord record) throws InterruptedException {

        List<StatisticPhase> statistics = new ArrayList<>();

        long intervalNs;
        if (record.mode().equals(LoadStrategy.BLOCKING)) {
            intervalNs = 0;
            log.info("Benchmark initialization with a closed model");
        } else {
            double requestsPerSecondPerConnection = record.requestsPerSecondPerConnection();
            intervalNs = (long) (Duration.ofSeconds(1).toNanos() / requestsPerSecondPerConnection);
            log.info("Benchmark initialization with an open model. requests_per_second_per_connection=" + requestsPerSecondPerConnection + ", max_requests_warmup_phase=" + (record.expectedWarmUpRequests()) + ", max_requests_test_phase=" + (record.expectedTestRequests()));
        }

        EventLoopGroup group = new NioEventLoopGroup(record.threads());

        ServerBootstrap sb = new ServerBootstrap();
        sb.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // Discard everything received (Black Hole)
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {}
                        });
                    }
                });

        // Bind to ephemeral port (0) to avoid conflicts
        Channel serverChannel = sb.bind("127.0.0.1", 0).sync().channel();
        int localPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
        log.info("Embedded anchor server started on port {}", localPort);

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ChannelPipeline p = ch.pipeline();

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

                            Scenario scenario = scenarioFactory.apply(ch.eventLoop());

                            if (record.mode().equals(LoadStrategy.BLOCKING)) {
                                p.addLast("run-handler", new BlockingLoadGenerator(ch.read(), scenario));
                            } else if (record.mode().equals(LoadStrategy.RATE_LIMIT)) {
                                p.addLast("run-handler", new RateLimitedBlockingLoadGenerator(ch.read(), scenario, intervalNs));
                            } else if (record.mode().equals(LoadStrategy.NON_BLOCKING)) {
                                p.addLast("run-handler", new NonBlockingLoadGenerator(ch.read(), scenario, intervalNs));
                            } else {
                                throw new IllegalStateException();
                            }
                        }
                    });

            Channel[] channels = new Channel[record.connections()];
            for (int i = 0; i < record.connections(); i++) {
                channels[i] = b.connect("127.0.0.1", localPort).sync().channel();
            }
            if (record.hasWarmUp()) {
                statistics.add(runWarmupPhase(channels, "warm-up", record.warmUpDuration()));
            }
            statistics.add(runTestPhase(channels, "test", record.duration()));

            group.shutdownGracefully();
            group.terminationFuture().sync();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Benchmark finished");

        return statistics;
    }

    private StatisticPhase runWarmupPhase(Channel[] channels, String name, Duration duration) throws InterruptedException {
        return this.runPhase(channels, name, duration);
    }

    private StatisticPhase runTestPhase(Channel[] channels, String name, Duration duration) throws InterruptedException {
        return this.runPhase(channels, name, duration);
    }

    /*
     * runPhase has block operations
     */
    private StatisticPhase runPhase(Channel[] channels, String name, Duration duration) throws InterruptedException {
        List<AbstractLoadGenerator> listeners = new ArrayList<>();
        for (Channel ch : channels) {
            AbstractLoadGenerator handler = (AbstractLoadGenerator) ch.pipeline().get("run-handler");
            listeners.add(handler);
        };
        log.info("Starting the phase: " + name);
        long start = System.nanoTime();
        long end = -1;
        listeners.forEach(h -> h.prepare(duration));
        listeners.forEach(AbstractLoadGenerator::start);
        Thread.sleep(duration);
        boolean requestsCompleted = false;
        while (!requestsCompleted) {
            int completedRequests = 0;
            for (AbstractLoadGenerator handler : listeners) {
                if (!handler.hasInflightRequests()) {
                    completedRequests++;
                    end = Math.max(end, handler.getEnd());
                    handler.stop();
                }
            }
            if (completedRequests == listeners.size()) {
                requestsCompleted = true;
            } else {
                Thread.sleep(1);
            }
        }

        List<Statistic> stats = new ArrayList<>();
        for (AbstractLoadGenerator listener : listeners) {
            stats.add(listener.collectStatistics());
        }
        log.info("Finished the phase: " + name);
        return new StatisticPhase(name, Duration.ofNanos(end - start), StatisticConverter.convert(stats));
    }
}
