package com.github.yewyc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask implements Callable<List<InstanceTask>> {

    private final long intervalNs;
    private final List<WeightTask> weightTasks;
    private final long totalDurationNs;
    private final double[] probabilities;
    private final long warmUpDurationNs;
    private final boolean recordWarmUp;

    public RunnableTask(long intervalNs, List<WeightTask> weightTasks, long warmUpDurationNs,
                        long steadyStateDurationNs, boolean recordWarmUp) {
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
    public List<InstanceTask> call() {

        // initialize
        List<InstanceTask> localWeightTasks = new ArrayList<>(this.weightTasks.size());
        for (WeightTask task : weightTasks) {
            InstanceTask instanceTask = new InstanceTask(task.initialize(), task.getProbability());
            localWeightTasks.add(instanceTask);
        }

        int i = 0;
        long start = System.nanoTime();

        while (true) {

            // when start
            long intendedTime;
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

            TaskStatus taskStatus = task.run();
            long end = System.nanoTime();

            boolean isWarmUpPhase = (end - start) < this.warmUpDurationNs;
            if (isWarmUpPhase) {
                if (this.recordWarmUp) {
                    task.recordValue(end - intendedTime, taskStatus);
                }
            } else {
                task.recordValue(end - intendedTime, taskStatus);
            }

            // stop?
            if (end - start > totalDurationNs) {
                break;
            }
        }
        return localWeightTasks;
    }
}
