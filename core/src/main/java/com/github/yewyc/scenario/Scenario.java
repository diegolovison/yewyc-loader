package com.github.yewyc.scenario;

import com.github.yewyc.sequence.Sequence;
import com.github.yewyc.step.BlockingStep;
import com.github.yewyc.step.Step;
import io.netty.channel.EventLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class Scenario {

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
        this.context = new ScenarioContext();
        this.sequences = new ArrayList<>();
    }

    public void start() {
        assert !this.eventLoop.inEventLoop();
        // must be empty
    }

    public void stop() {
        assert !this.eventLoop.inEventLoop();
        // must be empty
    }

    public CompletionStage<Void> execute(long id) {
        assert this.eventLoop.inEventLoop();
        Sequence sequence = getSequence();
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

        // Generate random value between 0 and totalWeight
        double randomValue = Math.random() * totalWeight;

        // Select sequence based on weight
        double cumulativeWeight = 0.0;
        for (Sequence sequence : sequences) {
            cumulativeWeight += sequence.getWeight();
            if (randomValue <= cumulativeWeight) {
                return sequence;
            }
        }

        // Fallback for floating point precision
        return sequences.get(sequences.size() - 1);
    }

    private CompletionStage<Void> runStep(int index, Sequence sequence, long id) {
        List<Step> steps = sequence.getSteps();
        if (index >= steps.size()) {
            return CompletableFuture.completedFuture(null);
        }

        Step step = steps.get(index);

        CompletionStage<Void> stepExecution;

        if (step instanceof BlockingStep) {
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
}
