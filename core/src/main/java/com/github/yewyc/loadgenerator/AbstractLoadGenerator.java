package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.stats.SequentialTimeSeriesRecorder;
import com.github.yewyc.stats.Statistic;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;

public abstract class AbstractLoadGenerator extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoadGenerator.class);

    private final Channel channel;
    protected final EventLoop eventLoop;
    private final Scenario scenario;

    private SequentialTimeSeriesRecorder localRecorder;
    private boolean running = false;
    protected long start;
    private long end;
    private Duration duration;
    protected long id;

    private final Queue<Integer> inflightRequests = new ArrayDeque<>();

    public AbstractLoadGenerator(Channel channel, Scenario scenario) {
        this.channel = channel;
        this.eventLoop = channel.eventLoop();
        this.scenario = scenario;
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
        assert !eventLoop.inEventLoop();
        eventLoop.execute(this::initializeAndScheduleNextRequest);
    }

    private void initializeAndScheduleNextRequest() {
        assert eventLoop.inEventLoop();
        this.running = true;
        this.id = 0;
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

    protected final void executeRequest(long whenFired, long intendedTime) {
        assert this.eventLoop.inEventLoop();
        if (whenFired > end) {
            this.running = false;
        } else if (channel.isWritable()) {
            inflightRequests.add(1);
            scenario.execute(this.id)
                    .whenCompleteAsync((result, error) -> {
                        long endTime = System.nanoTime();
                        inflightRequests.poll();
                        boolean success = (error == null);
                        if (!success) {
                            log.error("Scenario failed", error);
                        }
                        this.localRecorder.recordValue(whenFired, endTime - intendedTime, success);
                        onScenarioComplete();
                    }, eventLoop);

            this.id++;
        }
    }

    public boolean hasInflightRequests() {
        return !this.inflightRequests.isEmpty();
    }

    public Statistic collectStatistics() {
        return new Statistic(this.localRecorder.getHistograms(), this.localRecorder.getErrors());
    }

    public long getEnd() {
        return this.end;
    }

    protected abstract void onScenarioComplete();
}
