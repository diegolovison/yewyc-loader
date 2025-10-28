package com.github.yewyc;

import java.util.concurrent.Callable;

import static com.github.yewyc.CallableUtils.callTask;

public class WeightTask {

    private Callable<Task> task;
    private double probability;
    private Task instance;

    public WeightTask(Callable<Task> task, double probability) {
        this.task = task;
        this.probability = probability;
    }

    public Task getTask() {
        if (this.instance == null) {
            this.instance = callTask(task);
        }
        return this.instance;
    }

    public double getProbability() {
        return probability;
    }
}
