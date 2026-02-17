package com.github.yewyc.scenario;

import com.github.yewyc.sequence.Sequence;
import com.github.yewyc.stats.Statistic;
import com.github.yewyc.step.BlockingStep;
import com.github.yewyc.step.Step;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class Scenario {

    private static final Logger log = LoggerFactory.getLogger(Scenario.class);

    private static final AtomicInteger scenarioId = new AtomicInteger(42);

    private List<Sequence> sequences;
    private final EventLoop eventLoop;
    private ExecutorService workerPool;
    private final ScenarioContext context;

    public Scenario(EventLoop eventLoop) {
        this(eventLoop, null);
    }

    public Scenario(EventLoop eventLoop, ExecutorService workerPool) {
        this.eventLoop = eventLoop;
        this.workerPool = workerPool;
        this.context = new ScenarioContext(this.eventLoop, scenarioId.getAndIncrement());
        this.sequences = new ArrayList<>();
    }

    /**
     * slow operations
     * @param duration
     */
    public final void prepare(Duration duration) {
        assert !this.eventLoop.inEventLoop();
        for (Sequence sequence : sequences) {
            sequence.prepare(duration);
        }
    }

    /**
     * fast operations
     */
    public final void start(long start) {
        for (Sequence sequence : sequences) {
            sequence.start(start);
        }
    }

    public final void stop() {
        assert !this.eventLoop.inEventLoop();
    }

    public CompletionStage<Sequence> execute(long id, Sequence sequence) {
        assert this.eventLoop.inEventLoop();
        if (sequence == null) {
            sequence = getSequence();
        }
        return runStep(0, sequence, id);
    }

    private Sequence getSequence() {
        if (sequences.isEmpty()) {
            throw new IllegalStateException("No sequences available");
        }

        if (sequences.size() == 1) {
            return sequences.get(0);
        }

        // Calculate total weight
        double totalWeight = sequences.stream()
                .mapToDouble(Sequence::getWeight)
                .sum();

        // Select sequence based on weight
        int count = 0;
        while (count < 5) {
            double randomValue = this.getContext().getRandom().nextDouble() * totalWeight;
            double cumulativeWeight = 0.0;
            for (Sequence sequence : sequences) {
                cumulativeWeight += sequence.getWeight();
                if (randomValue <= cumulativeWeight) {
                    // the last one could be not ok
                    if (sequence.isOk(this.context)) {
                        return sequence;
                    }
                }
            }
            count++;
        }

        throw new IllegalStateException("Must be able to find a valid sequence");
    }

    private CompletionStage<Sequence> runStep(int index, Sequence sequence, long id) {
        List<Step> steps = sequence.getSteps();
        if (index >= steps.size()) {
            return CompletableFuture.completedFuture(sequence);
        }

        Step step = steps.get(index);

        CompletionStage<Void> stepExecution;

        if (step instanceof BlockingStep) {
            log.warn("BlockingStep was not tested.");
            if (workerPool == null) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Scenario contains a BlockingStep but no ExecutorService (workerPool) was provided.")
                );
            }
            stepExecution = CompletableFuture.runAsync(() -> {
                try {
                    // This call blocks the Worker Thread, NOT the Netty Thread
                    step.execute(context, this.eventLoop, id).toCompletableFuture().join();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, workerPool);
        } else {
            stepExecution = step.execute(context, this.eventLoop, id);
        }

        return stepExecution.thenComposeAsync(v -> runStep(index + 1, sequence, id), this.eventLoop);
    }

    public void addStep(Step step) {
        if (!this.sequences.isEmpty()) {
            throw new IllegalStateException("Cannot add more steps to a single step");
        }
        this.sequences = new ArrayList<>(1);
        this.sequences.add(new Sequence(1.0, List.of(step)));
    }

    public void setSequences(List<Sequence> sequences) {
        if (!this.sequences.isEmpty()) {
            throw new IllegalStateException("Cannot set sequences again");
        }
        this.sequences = sequences;
    }

    public ScenarioContext getContext() {
        return context;
    }

    public List<Statistic> collectStatistics() {
        List<Statistic> stats = new ArrayList<>();
        for (Sequence sequence : sequences) {
            Statistic stat = sequence.collectStatistics();
            stats.add(stat);
        }
        return stats;
    }
}
