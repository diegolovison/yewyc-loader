package com.github.yewyc.scenario;

import com.github.yewyc.step.BlockingStep;
import io.netty.channel.EventLoop;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class Scenario {

    private final List<ScenarioStep> steps;
    private final EventLoop eventLoop;
    private ExecutorService workerPool;
    private final ScenarioContext context;

    public Scenario(List<ScenarioStep> steps, EventLoop eventLoop) {
        this(steps, eventLoop, null);
    }

    public Scenario(List<ScenarioStep> steps, EventLoop eventLoop, ExecutorService workerPool) {
        this.steps = steps;
        this.eventLoop = eventLoop;
        this.workerPool = workerPool;
        this.context = new ScenarioContext();
    }

    public CompletionStage<Void> execute(long id) {
        assert this.eventLoop.inEventLoop();
        return runStep(0, id);
    }

    private CompletionStage<Void> runStep(int index, long id) {
        if (index >= steps.size()) {
            return CompletableFuture.completedFuture(null);
        }
        ScenarioStep step = steps.get(index);

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

        return stepExecution.thenComposeAsync(v -> runStep(index + 1, id), this.eventLoop);
    }
}
