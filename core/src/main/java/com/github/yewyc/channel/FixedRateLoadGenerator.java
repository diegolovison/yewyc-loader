package com.github.yewyc.channel;

import com.github.yewyc.stats.Statistics;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.github.yewyc.stats.Statistics.highestTrackableValue;
import static com.github.yewyc.stats.Statistics.numberOfSignificantValueDigits;

public class FixedRateLoadGenerator extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger log = LoggerFactory.getLogger(FixedRateLoadGenerator.class);

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

    private final EventLoop eventLoop;
    private final Queue<Long> latencyQueue = new ArrayDeque<>();

    public FixedRateLoadGenerator(URL urlBase, Channel channel, long intervalNs) {
        this.urlBase = urlBase;
        this.channel = channel;
        this.intervalNs = intervalNs;
        this.req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, this.urlBase.getPath(), Unpooled.EMPTY_BUFFER);
        this.req.headers().set(HttpHeaderNames.HOST, this.urlBase.getHost());
        this.req.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");

        this.eventLoop = channel.eventLoop();
    }

    public void start(String name) {
        this.reset();

        this.name = name;
        this.start = System.currentTimeMillis();
        this.startIntendedTime = System.nanoTime();
        this.running = true;

        if (eventLoop.inEventLoop()) {
            loopSend();
        } else {
            eventLoop.execute(this::loopSend);
        }
    }

    private void loopSend() {
        if (!running) return;
        long intendedTime = startIntendedTime + (i * this.intervalNs);
        long now = System.nanoTime();
        long delayNs = intendedTime - now;
        if (delayNs > 0) {
            eventLoop.schedule(this::loopSend, delayNs, TimeUnit.NANOSECONDS);
        } else {
            sendRequest(intendedTime);
            i++;
            eventLoop.execute(this::loopSend);
        }
    }

    private void sendRequest(long intendedTime) {
        if (!channel.isWritable()) {
            log.warn("Channel not writable! Client is overloaded.");
        }
        // within event loop and multiple writes
        this.latencyQueue.add(intendedTime);
        channel.writeAndFlush(req.retainedDuplicate());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        if (!running) return;
        Long startTime = this.latencyQueue.poll();
        long elapsedTimeNs = System.nanoTime() - startTime;
        this.recorder.recordValue(elapsedTimeNs);

        if (msg.status().code() != 200) {
            this.errorCount += 1;
        }
        if (log.isTraceEnabled()) {
            String responseBody = msg.content().toString(io.netty.util.CharsetUtil.UTF_8);
            log.trace("Response [" + msg.status().code() + "]: " + responseBody);
        }

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
    }

    public void stop() {
        this.running = false;
        this.end = System.currentTimeMillis();
    }

    private void reset() {
        this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
        this.lastRecordedTimeForGroupingHistograms = 0;
        this.errorCount = 0;
        this.i = 0;
        this.histograms = new ArrayList<>();
        this.errors = new ArrayList<>();

        this.start = 0;
        this.startIntendedTime = 0;
        this.end = 0;

        this.latencyQueue.clear();
    }

    public Statistics collectStatistics() {
        return new Statistics(this.name, this.start, this.end, this.histograms, this.errors);
    }
}
