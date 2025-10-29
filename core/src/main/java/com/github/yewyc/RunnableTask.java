package com.github.yewyc;

import java.util.List;

import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask implements Runnable {

    private final long intervalNs;
    private final List<WeightTask> weightTasks;
    private final long totalDurationNs;
    private final double[] probabilities;
    private final long warmUpDurationNs;
    private final long steadyStateDurationNs;
    private final boolean recordWarmUp;

    public RunnableTask(long intervalNs, List<WeightTask> weightTasks, long warmUpDurationNs,
                        long steadyStateDurationNs, double[] probabilities, boolean recordWarmUp) {
        this.intervalNs = intervalNs;
        this.weightTasks = weightTasks;
        this.warmUpDurationNs = warmUpDurationNs;
        this.steadyStateDurationNs = steadyStateDurationNs;
        this.totalDurationNs = this.warmUpDurationNs +  this.steadyStateDurationNs;
        this.probabilities = probabilities;
        this.recordWarmUp = recordWarmUp;
    }

    @Override
    public void run() {
        int i = 0;
        long start = System.nanoTime();

        while (true) {

            // when start
            long intendedTime = start + (i++) * this.intervalNs;
            long now;
            while ((now = System.nanoTime()) < intendedTime)
                LockSupport.parkNanos(intendedTime - now);

            // request
            long taskStarted = System.nanoTime();

            Task task;
            if (this.weightTasks.size() > 1) {
                int prob = cdfChoice(this.probabilities);
                task = this.weightTasks.get(prob).getTask();
            } else {
                task = this.weightTasks.get(0).getTask();
            }

            task.addBlockedTime(taskStarted - intendedTime);
            TaskStatus taskStatus = task.run();
            long end = System.nanoTime();

            boolean isWarmUpPhase = (end - start) < this.warmUpDurationNs;
            if (isWarmUpPhase) {
                if (this.recordWarmUp) {
                    task.recordValue(end, end - intendedTime, taskStatus);
                }
            } else {
                task.recordValue(end, end - intendedTime, taskStatus);
            }

            // stop?
            if (end - start > totalDurationNs) {
                break;
            }
        }
    }
}
