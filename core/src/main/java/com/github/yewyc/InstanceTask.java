package com.github.yewyc;

public class InstanceTask {

    private final Task task;
    private final double probability;

    public InstanceTask(Task task, double probability) {
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
