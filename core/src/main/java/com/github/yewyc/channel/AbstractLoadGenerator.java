package com.github.yewyc.channel;

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
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.github.yewyc.stats.Statistic.highestTrackableValue;
import static com.github.yewyc.stats.Statistic.numberOfSignificantValueDigits;

/**
 * Abstract base class for HTTP load generators implementing the Template Method pattern.
 *
 * <h2>Architecture Overview</h2>
 * This class provides the framework for generating HTTP load against a target server,
 * with subclasses defining the specific scheduling strategy (closed model vs open model).
 *
 * <h2>Control Flow</h2>
 * <pre>
 * 1. {@link #start(String, Duration)} - Public entry point, initializes the generator
 *    ↓
 * 2. {@link #initializeAndScheduleNextRequest()} - Private orchestration method
 *    - Initializes timing on first call
 *    - Delegates to scheduling strategy
 *    ↓
 * 3. {@link #scheduleNextRequest()} - Abstract template method (subclass implements)
 *    - Defines WHEN to send the next request
 *    - Closed model: immediately after response
 *    - Open model: at fixed rate intervals
 *    ↓
 * 4. {@link #executeRequest(long)} - Final helper method
 *    - Performs the actual HTTP request
 *    - Records timing for latency measurement
 *    ↓
 * 5. {@link #channelRead0(ChannelHandlerContext, FullHttpResponse)} - Response handler
 *    - Records latency and statistics
 *    - May trigger next request (closed model)
 * </pre>
 *
 * <h2>Extension Points</h2>
 * Subclasses must implement:
 * <ul>
 *   <li>{@link #scheduleNextRequest()} - Define the scheduling strategy</li>
 * </ul>
 *
 * <h2>Load Models</h2>
 * <ul>
 *   <li><b>Closed Model</b> ({@link LoadGenerator}): Sends next request immediately after
 *       receiving response. Maintains constant number of concurrent requests.</li>
 *   <li><b>Open Model</b> ({@link FixedRateLoadGenerator}): Sends requests at fixed rate
 *       regardless of responses. Simulates independent user arrivals.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * All operations are executed on the Netty event loop thread. The {@link #start(Duration)}
 * method must be called from outside the event loop and will schedule work appropriately.
 *
 * @see LoadGenerator
 * @see FixedRateLoadGenerator
 */
public abstract class AbstractLoadGenerator extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoadGenerator.class);
    protected static final long nan = Long.MIN_VALUE;

    private final Channel channel;
    private final FullHttpRequest req;

    private Recorder recorder;
    private boolean running = false;
    private long end;
    private long lastRecordedTimeForGroupingHistograms = 0;
    private int errorCount = 0;
    private List<Histogram> histograms;
    private List<Integer> errors;
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

    public void start(Duration duration) {
        this.duration = duration;
        assert !eventLoop.inEventLoop();
        eventLoop.execute(this::initializeAndScheduleNextRequest);
    }

    private void initializeAndScheduleNextRequest() {

        assert eventLoop.inEventLoop();

        this.reset();

        this.recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
        this.lastRecordedTimeForGroupingHistograms = 0;
        this.errorCount = 0;
        this.histograms = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.running = true;
        this.end = System.nanoTime() + this.duration.toNanos();
        this.counter = 0;
        scheduleNextRequest();
    }

    protected final void scheduleNextRequestIfRunning() {
        if (running) {
            scheduleNextRequest();
        }
    }

    protected abstract void scheduleNextRequest();

    protected final void executeRequest(long intendedTime) {

        if (intendedTime > end) {
            this.running = false;
        } else {
            if (!channel.isWritable()) {
                log.warn("Channel not writable! Client is overloaded.");
            }
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
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Long[] data = this.latencyQueue.poll();
        assert data != null;
        long localId = data[0];
        long startTime = data[1];
        // NPE cannot happen here
        this.recorder.recordValue(System.nanoTime() - startTime);

        if (msg.status().code() != 200) {
            this.errorCount += 1;
        }
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

    protected void reset() {

    }

    public Statistic collectStatistics() {
        return new Statistic(this.histograms, this.errors);
    }

    public boolean hasInflightRequests() {
        return !this.latencyQueue.isEmpty();
    }
}
