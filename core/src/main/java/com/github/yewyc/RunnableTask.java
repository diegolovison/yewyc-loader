package com.github.yewyc;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask implements Callable<RunnableResult> {

    private static final Logger log = Logger.getLogger(RunnableTask.class);

    private int id;
    private final long intervalNs;
    private final List<WeightTask> weightTasks;
    private final long totalDurationNs;
    private final double[] probabilities;
    private final long warmUpDurationNs;
    private final boolean recordWarmUp;

    public RunnableTask(int id, long intervalNs, List<WeightTask> weightTasks, long warmUpDurationNs,
                        long steadyStateDurationNs, boolean recordWarmUp) {
        this.id = id;
        this.intervalNs = intervalNs;
        this.weightTasks = weightTasks;
        this.warmUpDurationNs = warmUpDurationNs;
        this.totalDurationNs = this.warmUpDurationNs +  steadyStateDurationNs;
        this.recordWarmUp = recordWarmUp;

        this.probabilities = this.weightTasks.stream()
                .mapToDouble(WeightTask::getProbability)
                .toArray();
        double sum = Arrays.stream(probabilities).sum();
        if (sum > 1.0) {
            throw new IllegalStateException("The sum of the probabilities cannot be greater than 1.0");
        }
    }

    @Override
    public RunnableResult call() {
        long start = System.nanoTime();
        long end;
        List<InstanceTask> localWeightTasks = new ArrayList<>(this.weightTasks.size());
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean started = new AtomicBoolean(true);
        try {
            log.info("Starting: " + this);

            // create a task instance
            for (WeightTask task : weightTasks) {
                InstanceTask instanceTask = new InstanceTask(task.initialize(), task.getProbability());
                localWeightTasks.add(instanceTask);
            }

            int i = 0;

            while (true) {

                // when start
                final long intendedTime;
                if (this.intervalNs == Model.CLOSED_MODEL.value) {
                    intendedTime = System.nanoTime();
                } else {
                    intendedTime = start + (i++) * this.intervalNs;
                    long now;
                    while ((now = System.nanoTime()) < intendedTime)
                        LockSupport.parkNanos(intendedTime - now);
                }

                Task task;
                if (localWeightTasks.size() > 1) {
                    int prob = cdfChoice(this.probabilities);
                    task = localWeightTasks.get(prob).getTask();
                } else {
                    task = localWeightTasks.getFirst().getTask();
                }

                CompletableFuture<TaskStatus> taskStatusFuture = task.submit();
                counter.incrementAndGet();
                taskStatusFuture.whenComplete((taskStatus, ex) -> {
                    if (!started.get()) {
                        return;
                    }
                    long localEnd = System.nanoTime();
                    // success
                    if (ex == null) {
                        boolean isWarmUpPhase = this.warmUpDurationNs > 0 && (localEnd - start) < this.warmUpDurationNs;
                        if (isWarmUpPhase) {
                            if (this.recordWarmUp) {
                                task.recordValue(localEnd - intendedTime, taskStatus);
                            }
                        } else {
                            task.recordValue(localEnd - intendedTime, taskStatus);
                        }
                    } else {
                        // log ?
                        task.recordValue(localEnd - intendedTime, TaskStatus.FAILED);
                    }
                    counter.decrementAndGet();
                });

                end = System.nanoTime();

                // stop?
                if (end - start > totalDurationNs) {
                    started.set(false);
                    break;
                }
            }
            log.info("Finished " + this + ". Remaining tasks: " + counter.get());
        } finally {
            for (InstanceTask instanceTask : localWeightTasks) {
                instanceTask.getTask().close();
            }
        }

        log.info("Closed the resources: " + this);
        return new RunnableResult(start, end, localWeightTasks);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.id + ")";
    }
}
