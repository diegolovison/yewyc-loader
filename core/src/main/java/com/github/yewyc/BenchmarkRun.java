package com.github.yewyc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.jboss.logging.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.Task.highestTrackableValue;
import static com.github.yewyc.Task.numberOfSignificantValueDigits;

public class BenchmarkRun {

    private static final AttributeKey<Long> beginAttributeKey = AttributeKey.newInstance("begin");

    private static final Logger log = Logger.getLogger(Benchmark.class);

    public List<Statistics> run(int rate, int connections, int threads, String urlBaseParam, Duration duration, Duration warmUpDuration) {

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
            List<StatsChannelInboundHandler> listeners = new ArrayList<>();
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            StatsChannelInboundHandler last = new StatsChannelInboundHandler(urlBase, ch.read(), intervalNs);
                            ChannelPipeline p = ch.pipeline();

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
                            p.addLast(last);
                            listeners.add(last);
                        }
                    });

            for (int i = 0; i < connections; i++) {
                b.connect(urlBase.getHost(), urlBase.getPort()).sync().channel();
            }

            if (warmUpDuration != null) {
                tasks.add(runPhase(listeners, "warm-up", warmUpDuration));
            }
            tasks.add(runPhase(listeners, "test", duration));

            group.shutdownGracefully();
            group.terminationFuture().sync();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Benchmark finished");

        return tasks;
    }

    private Statistics runPhase(List<StatsChannelInboundHandler> listeners, String name, Duration duration) throws InterruptedException {

        listeners.forEach(h -> h.start(name));
        log.info("Starting the phase: " + name);
        Thread.sleep(duration.toMillis());
        listeners.forEach(StatsChannelInboundHandler::stop);

        List<Statistics> stats = new ArrayList<>();
        for (StatsChannelInboundHandler listener : listeners) {
            stats.add(listener.collectStatistics());
        }
        Statistics stat = stats.getFirst();
        for (int i = 1; i < stats.size(); i++) {
            stat.merge(stats.get(i));
        }
        log.info("Finished the phase: " + name);
        return stat;
    }

    private static final class StatsChannelInboundHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final URL urlBase;
        private final Channel channel;
        private final long intervalNs;
        private final FullHttpRequest req;

        private Recorder recorder;
        private boolean running = false;
        private long start;
        private long end;
        private long startIntendedTime;
        private long lastRecordedTimeForGroupingHistograms = 0;
        private int errorCount = 0;
        private int i = 0;
        private List<Histogram> histograms;
        private List<Integer> errors;
        private String name;

        private StatsChannelInboundHandler(URL urlBase, Channel channel, long intervalNs) {
            this.urlBase = urlBase;
            this.channel = channel;
            this.intervalNs = intervalNs;
            this.req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, this.urlBase.getPath(), Unpooled.EMPTY_BUFFER);
            this.req.headers().set(HttpHeaderNames.HOST, this.urlBase.getHost());
            this.req.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {

            long elapsedTimeNs = System.nanoTime() - ctx.channel().attr(beginAttributeKey).get();
            // assert ctx.channel().attr(firedAttributeKey).get();
            this.recorder.recordValue(elapsedTimeNs);
            if (msg.status().code() != 200) {
                this.errorCount += 1;
            }

            ctx.channel().attr(beginAttributeKey).set(null);
            //ctx.channel().attr(firedAttributeKey).set(null);

            long now = System.currentTimeMillis();
            if (lastRecordedTimeForGroupingHistograms == 0) {
                lastRecordedTimeForGroupingHistograms = now;
            }
            long elapsedTimeForGroupingHistagrams = now - this.lastRecordedTimeForGroupingHistograms;
            if (elapsedTimeForGroupingHistagrams >= 1000) {
                this.histograms.add(recorder.getIntervalHistogram());
                this.errors.add(errorCount);
                this.lastRecordedTimeForGroupingHistograms = now;
                this.errorCount = 0;
            }

            sendRequest(ctx.channel());
        }

        public void start(String name) {
            this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
            this.running = true;
            this.start = System.currentTimeMillis();
            this.startIntendedTime = System.nanoTime();
            this.lastRecordedTimeForGroupingHistograms = 0;
            this.errorCount = 0;
            this.i = 0;
            this.histograms = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.name = name;

            this.sendRequest(this.channel);
        }

        private void sendRequest(Channel ch) {
            if (this.running) {

                final long intendedTime;
                if (this.intervalNs == 0) {
                    intendedTime = System.nanoTime();
                } else {
                    intendedTime = startIntendedTime + (i++) * this.intervalNs;
                    long now;
                    while ((now = System.nanoTime()) < intendedTime)
                        LockSupport.parkNanos(intendedTime - now);
                }
                ch.attr(beginAttributeKey).set(intendedTime);
                ch.writeAndFlush(req);
                //ch.attr(firedAttributeKey).set(false);
                /*
                ch.writeAndFlush(req).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        channelFuture.channel().attr(firedAttributeKey).set(true);
                    }
                });
                 */
            }
        }

        public void stop() {
            this.running = false;
            this.end = System.currentTimeMillis();
        }

        public Statistics collectStatistics() {
            return new Statistics(this.name, this.start, this.end, this.histograms, this.errors);
        }
    }
}
