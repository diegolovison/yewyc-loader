package com.github.yewyc;

import java.util.UUID;
import java.util.concurrent.Callable;

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

    public Task initialize() {
        Task instance = callTask(this.task);
        instance.setId(this.id);
        return instance;
    }

    public double getProbability() {
        return probability;
    }
}
