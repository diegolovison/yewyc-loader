package com.github.yewyc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.LockSupport;

public class RunnableTask implements Runnable {

    private final long intervalNs;
    private final List<Task> tasks;
    private final long timeNs;
    private final MeasureLatencyType latencyType;
    private final ThreadPoolExecutor recordExecutor;

    public RunnableTask(long intervalNs, List<Task> tasks, long timeNs, MeasureLatencyType latencyType, ThreadPoolExecutor recordExecutor) {
        this.intervalNs = intervalNs;
        this.tasks = tasks;
        this.timeNs = timeNs;
        this.latencyType = latencyType;
        this.recordExecutor = recordExecutor;
    }

    @Override
    public void run() {
        int i = 0;
        long start = System.nanoTime();
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
            for (int j = 0; j < this.tasks.size(); j++) {
                Task task = this.tasks.get(j);
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
        if (MeasureLatencyType.GLOBAL.equals(this.latencyType)) {
            CompletableFuture.runAsync(() -> task.recordValue(end, end - intendedTime), recordExecutor);
        } else if(MeasureLatencyType.INDIVIDUAL.equals(this.latencyType)) {
            long finalTaskElapsed = taskElapsed;
            CompletableFuture.runAsync(() -> task.recordValue(end, end - intendedTime - finalTaskElapsed), recordExecutor);
            taskElapsed = end - taskStarted;
        } else {
            throw new RuntimeException(this.latencyType + " not implemented");
        }
        return taskElapsed;
    }
}
