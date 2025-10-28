package com.github.yewyc;

import java.util.List;

import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask implements Runnable {

    private final long intervalNs;
    private final List<WeightTask> weightTasks;
    private final long timeNs;
    private final double[] probabilities;

    public RunnableTask(long intervalNs, List<WeightTask> weightTasks, long timeNs, double[] probabilities) {
        this.intervalNs = intervalNs;
        this.weightTasks = weightTasks;
        this.timeNs = timeNs;
        this.probabilities = probabilities;
    }

    @Override
    public void run() {
        int i = 0;
        long start = System.nanoTime();

        while (true) {

            // when start
            long intendedTime = start + (i++) * intervalNs;
            long now;
            while ((now = System.nanoTime()) < intendedTime)
                LockSupport.parkNanos(intendedTime - now);

            // request
            long taskStarted = System.nanoTime();

            Task task;
            if (this.weightTasks.size() > 1) {
                int prob = cdfChoice(probabilities);
                task = this.weightTasks.get(prob).getTask();
            } else {
                task = this.weightTasks.get(0).getTask();
            }

            task.addBlockedTime(taskStarted - intendedTime);
            task.run();

            long end = System.nanoTime();
            // stop?
            if (end - start > timeNs) {
                break;
            }

            task.recordValue(end, end - intendedTime);
        }
    }
}
