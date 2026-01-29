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

import static com.github.yewyc.stats.Statistics.highestTrackableValue;
import static com.github.yewyc.stats.Statistics.numberOfSignificantValueDigits;

public abstract class AbstractLoadGenerator extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private static final Logger log = LoggerFactory.getLogger(AbstractLoadGenerator.class);
    protected static final long nan = 0;

    protected final Channel channel;
    protected final FullHttpRequest req;

    protected Recorder recorder;
    protected boolean running = false;
    protected long start;
    protected long end;
    protected long startIntendedTime;
    protected long lastRecordedTimeForGroupingHistograms = 0;
    protected int errorCount = 0;
    protected List<Histogram> histograms;
    protected List<Integer> errors;
    protected String name;

    protected final EventLoop eventLoop;
    protected final Queue<Long> latencyQueue = new ArrayDeque<>();

    public AbstractLoadGenerator(URL urlBase, Channel channel) {
        this.channel = channel;
        this.req = new DefaultFullHttpRequest(httpVersion, HttpMethod.GET, urlBase.getPath(), Unpooled.EMPTY_BUFFER);
        this.req.headers().set(HttpHeaderNames.HOST, urlBase.getHost());
        this.req.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");

        this.eventLoop = channel.eventLoop();
    }

    public void start(String name) {
        this.reset();
        this.name = name;
        this.running = true;
        assert !eventLoop.inEventLoop();
        eventLoop.execute(this::loopSend);
    }

    protected void loopSend() {
        if (!running) return;
        if (startIntendedTime == nan) {
            this.start = System.currentTimeMillis();
            this.startIntendedTime = System.nanoTime();
        }
        send();
    }

    protected abstract void send();

    protected void sendRequest(long intendedTime) {
        if (!channel.isWritable()) {
            log.warn("Channel not writable! Client is overloaded.");
        }

        /*
         * If you use a single AttributeKey, you are effectively using a single variable to store the "intended time."
         * When you send multiple requests in parallel (pipelining), the second request will overwrite the
         * start time of the first request.
         *
         * Because HTTP/1.1 guarantees that responses arrive in the same order requests were sent (FIFO),
         * you should use a simple Queue instead of a channel attribute.
         *
         */
        assert httpVersion == HttpVersion.HTTP_1_1;
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
        assert this.end == nan;
        this.end = System.currentTimeMillis();
    }

    protected void reset() {
        this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
        this.lastRecordedTimeForGroupingHistograms = 0;
        this.errorCount = 0;
        this.histograms = new ArrayList<>();
        this.errors = new ArrayList<>();

        this.start = nan;
        this.startIntendedTime = nan;
        this.end = nan;

        this.latencyQueue.clear();
    }

    public Statistics collectStatistics() {
        return new Statistics(this.name, this.start, this.end, this.histograms, this.errors);
    }
}
