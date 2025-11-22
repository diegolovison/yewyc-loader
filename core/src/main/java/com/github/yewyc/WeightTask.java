package com.github.yewyc;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.github.yewyc.CallableUtils.callTask;

public class WeightTask {

    private final String id;
    private final Callable<Task> task;
    private final double probability;

    public WeightTask(Callable<Task> task) {
        this(task, 1.0);
    }

    public WeightTask(Callable<Task> task, double probability) {
        this.id = UUID.randomUUID().toString();
        this.task = task;
        this.probability = probability;
    }

    public Task initialize(ExecutorService executor) {
        Task instance = callTask(this.task);
        instance.initialize(executor);
        instance.setId(this.id);
        return instance;
    }

    public double getProbability() {
        return probability;
    }
}
