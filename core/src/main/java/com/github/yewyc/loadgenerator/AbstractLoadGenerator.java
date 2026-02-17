package com.github.yewyc.loadgenerator;

import com.github.yewyc.scenario.Scenario;
import com.github.yewyc.sequence.Sequence;
import com.github.yewyc.stats.StatisticSequence;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public abstract class AbstractLoadGenerator extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoadGenerator.class);

    private final Channel channel;
    protected final EventLoop eventLoop;
    private final Scenario scenario;

    private boolean running = false;
    protected long start;
    private long end;
    private Duration duration;
    protected long id;

    private Sequence next;

    private int inflightRequests = 0;

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
        this.scenario.prepare(duration);
        return this;
    }

    /**
     * delegate. in this case, the SimpleChannelInboundHandler will have small diff when compared with others
     * super fast operations
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
        this.scenario.start(start);
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
            inflightRequests += 1;
            scenario.execute(this.id, this.next)
                    .whenCompleteAsync((sequence, error) -> {
                        long endTime = System.nanoTime();
                        inflightRequests -= 1;
                        assert intendedTime >= 0;
                        boolean success = (error == null);
                        if (!success) {
                            log.error("Scenario failed", error);
                        }
                        sequence.recordValue(whenFired, intendedTime, endTime, success);
                        if (sequence.getNext() != null && sequence.getNext().isOk(scenario.getContext())) {
                            this.next = sequence.getNext();
                        } else {
                            this.next = null;
                        }
                        onScenarioComplete();
                    }, eventLoop);

            this.id++;
        }
    }

    public boolean hasInflightRequests() {
        return this.inflightRequests > 0;
    }

    /**
     * per seequence
     * @return
     */
    public List<StatisticSequence> collectStatistics() {
        return this.scenario.collectStatistics();
    }

    public long getEnd() {
        return this.end;
    }

    protected abstract void onScenarioComplete();

    public void stop() {
        this.scenario.stop();
    }
}
