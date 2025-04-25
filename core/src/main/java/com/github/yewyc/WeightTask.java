package com.github.yewyc;

import java.util.List;

public class WeightTask {
    private List<Task> tasks;
    private double probability;

    public WeightTask(List<Task> tasks, double probability) {
        this.tasks = tasks;
        this.probability = probability;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public double getProbability() {
        return probability;
    }
}
