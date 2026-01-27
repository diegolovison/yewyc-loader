package com.github.yewyc;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.jboss.logging.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.Task.highestTrackableValue;
import static com.github.yewyc.Task.numberOfSignificantValueDigits;

public class RunChannelInboundHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger log = Logger.getLogger(RunChannelInboundHandler.class);

    private static final AttributeKey<Long> beginAttributeKey = AttributeKey.newInstance("begin");

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

    public RunChannelInboundHandler(URL urlBase, Channel channel, long intervalNs) {
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
        if (log.isTraceEnabled()) {
            String responseBody = msg.content().toString(io.netty.util.CharsetUtil.UTF_8);
            log.trace("Response [" + msg.status().code() + "]: " + responseBody);
        }

        ctx.channel().attr(beginAttributeKey).set(null);

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
