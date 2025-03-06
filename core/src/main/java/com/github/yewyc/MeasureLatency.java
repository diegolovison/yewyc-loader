package com.github.yewyc;

import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class MeasureLatency {

    private static final Logger LOGGER = Logger.getLogger(MeasureLatency.class);

    private long timeNs = -1;
    private long intervalNs;
    private int virtualThreads;
    private Task task;

    public MeasureLatency addTask(Task task) {
        this.task = task;
        return this;
    }

    public MeasureLatency start(long timeSec, int opsPerSec, int virtualThreads) {
        if (virtualThreads < 0) {
            throw new RuntimeException("virtualThreads must be equals or greater than 1");
        }
        if (opsPerSec < 0) {
            throw new RuntimeException("opsPerSec must be equals or greater than 0");
        } else if (opsPerSec == 0) {
            LOGGER.warn("Are you aware of what you are doing?");
        }
        this.timeNs = TimeUnit.SECONDS.toNanos(timeSec);
        if (opsPerSec > 0) {
            this.intervalNs = 1000000000 / opsPerSec;
        }
        this.virtualThreads = virtualThreads;

        this.task.configure(this.timeNs);

        this.measure();

        return this;
    }

    private MeasureLatency measure() {

        Runnable wrapperTask = () -> {
            int i = 0;
            long start = System.nanoTime();
            while (true) {

                long intendedTime;
                if (this.intervalNs > 0) {
                    intendedTime = start + (i++) * intervalNs;
                    long now;
                    while ((now = System.nanoTime()) < intendedTime)
                        LockSupport.parkNanos(intendedTime - now);
                } else {
                    intendedTime = System.nanoTime();
                }

                // request
                this.task.getAction().run();

                if (System.nanoTime() - start < timeNs) {
                    long end = System.nanoTime();
                    this.task.recordValue(end - intendedTime);
                } else {
                    break;
                }
            }
        };

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < this.virtualThreads; i++) {
                executor.submit(wrapperTask);
            }
        } // The executor automatically shuts down here
        return this;
    }

    public void generateReport() {
        this.task.report(this.intervalNs);
    }
}