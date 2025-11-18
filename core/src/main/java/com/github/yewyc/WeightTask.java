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

    public void initialize() {
        synchronized (this) {
            if (this.instance == null) {
                this.instance = callTask(this.task);
            }
        }
    }

    public Task getTask() {
        return this.instance;
    }

    public double getProbability() {
        return probability;
    }
}
