package com.github.yewyc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.LockSupport;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask implements Runnable {

    private final long intervalNs;
    private final List<WeightTask> weightTasks;
    private final long timeNs;
    private final ThreadPoolExecutor recordExecutor;
    private final double[] probabilities;

    public RunnableTask(long intervalNs, List<WeightTask> weightTasks, long timeNs, ThreadPoolExecutor recordExecutor, double[] probabilities) {
        this.intervalNs = intervalNs;
        this.weightTasks = weightTasks;
        this.timeNs = timeNs;
        this.recordExecutor = recordExecutor;
        this.probabilities = probabilities;
    }

    @Override
    public void run() {
        int i = 0;
        long start = System.nanoTime();

        List<Task> tasks = null;
        if (this.weightTasks.size() == 1) {
            tasks = this.weightTasks.getFirst().getTasks();
        }

        outer:
        while (true) {

            // when start
            long intendedTime = start + (i++) * intervalNs;
            long now;
            while ((now = System.nanoTime()) < intendedTime)
                LockSupport.parkNanos(intendedTime - now);

            // request
            long taskStarted = System.nanoTime();
            long taskElapsed = 0;

            if (this.weightTasks.size() > 1) {
                int prob = cdfChoice(probabilities);
                tasks = this.weightTasks.get(prob).getTasks();
            }

            for (int j = 0; j < tasks.size(); j++) {
                Task task = tasks.get(j);
                if (j == 0) {
                    task.addBlockedTime(taskStarted - intendedTime);
                }
                task.run();
                long end = System.nanoTime();
                // stop?
                if (end - start > timeNs) {
                    break outer;
                }
                taskElapsed = record(task, end, intendedTime, taskElapsed, taskStarted);
            }
        }
    }

    private long record(Task task, long end, long intendedTime, long taskElapsed, long taskStarted) {
        long finalTaskElapsed = taskElapsed;
        CompletableFuture.runAsync(() -> task.recordValue(end, end - intendedTime - finalTaskElapsed), recordExecutor);
        taskElapsed = end - taskStarted;
        return taskElapsed;
    }
}
