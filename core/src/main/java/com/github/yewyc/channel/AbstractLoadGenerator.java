package com.github.yewyc.channel;

import com.github.yewyc.stats.SequentialTimeSeriesRecorder;
import com.github.yewyc.stats.Statistic;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;

public abstract class AbstractLoadGenerator extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoadGenerator.class);
    protected static final long nan = Long.MIN_VALUE;

    private final Channel channel;
    private final FullHttpRequest req;

    private SequentialTimeSeriesRecorder localRecorder;
    private boolean running = false;
    private long start;
    private long end;
    private Duration duration;
    private long counter;

    protected final EventLoop eventLoop;
    private final Queue<Long[]> latencyQueue = new ArrayDeque<>();

    private final boolean assertResponseOperation;

    public AbstractLoadGenerator(URL urlBase, Channel channel) {
        this.assertResponseOperation = System.getProperty("assertResponseOperation", "false").equals("true");
        if (this.assertResponseOperation) {
            log.info("Assert response operation enabled");
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
        HttpVersion httpVersion = HttpVersion.HTTP_1_1;

        this.channel = channel;
        this.req = new DefaultFullHttpRequest(httpVersion, HttpMethod.GET, urlBase.getPath(), Unpooled.EMPTY_BUFFER);
        this.req.headers().set(HttpHeaderNames.HOST, urlBase.getHost());
        this.req.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");

        this.eventLoop = channel.eventLoop();
    }

    /**
     * It can contains slow operation
     * @param duration
     */
    public AbstractLoadGenerator prepare(Duration duration) {
        assert !eventLoop.inEventLoop();
        this.duration = duration;
        this.localRecorder = new SequentialTimeSeriesRecorder(this.duration);
        return this;
    }

    /**
     * delegate. in this case, the SimpleChannelInboundHandler will have small diff when compared with others
     */
    public void start() {
        eventLoop.execute(this::initializeAndScheduleNextRequest);
    }

    private void initializeAndScheduleNextRequest() {
        assert eventLoop.inEventLoop();
        this.reset();
        this.running = true;
        this.counter = 0;
        this.start = System.nanoTime();
        this.localRecorder.start(this.start);
        this.end = this.start + this.duration.toNanos();
        scheduleNextRequest();
    }

    protected final void scheduleNextRequestIfRunning() {
        assert this.eventLoop.inEventLoop();
        if (running) {
            scheduleNextRequest();
        }
    }

    protected abstract void scheduleNextRequest();

    protected final boolean executeRequest(long intendedTime) {
        assert this.eventLoop.inEventLoop();
        boolean ok = true;
        if (intendedTime > end) {
            this.running = false;
            ok = false;
        } else if (!channel.isWritable()) {
            ok = false;
        } else {
            this.latencyQueue.add(new Long[]{counter, intendedTime});
            FullHttpRequest localRequest = this.req.retainedDuplicate();
            if (this.assertResponseOperation) {
                localRequest.headers().set("X-Request-Id", this.counter);
            }
            channel.writeAndFlush(localRequest).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to send request {}", counter, future.cause());
                }
            });
            counter++;
        }
        return ok;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Long[] data = this.latencyQueue.poll();
        assert data != null;
        long localId = data[0];
        long startTime = data[1];
        boolean success = true;
        if (msg.status().code() != 200) {
            success = false;
        }
        this.localRecorder.recordValue(startTime, System.nanoTime() - startTime, success);
        if (log.isTraceEnabled()) {
            String responseBody = msg.content().toString(io.netty.util.CharsetUtil.UTF_8);
            log.trace("Response [" + msg.status().code() + "]: " + responseBody);
        }
        if (this.assertResponseOperation) {
            String responseBody = msg.content().toString(io.netty.util.CharsetUtil.UTF_8);
            if (!responseBody.equals(String.valueOf(localId))) {
                throw new RuntimeException("Invalid id");
            }
        }
    }

    protected void reset() {

    }

    public Statistic collectStatistics() {
        return new Statistic(this.localRecorder.getHistograms(), this.localRecorder.getErrors());
    }

    public boolean hasInflightRequests() {
        return !this.latencyQueue.isEmpty();
    }

    public long getEnd() {
        return this.end;
    }
}
