package com.github.yewyc;

public class WeightTask {
    private Task task;
    private double probability;

    public WeightTask(Task task, double probability) {
        this.task = task;
        this.probability = probability;
    }

    public Task getTask() {
        return task;
    }

    public double getProbability() {
        return probability;
    }
}
