package com.github.yewyc.sequence;

import com.github.yewyc.step.Step;

import java.util.List;

public class Sequence {

    private double weight;
    private List<Step> steps;

    public Sequence(double weight, List<Step> steps) {
        this.weight = weight;
        this.steps = steps;
    }

    public double getWeight() {
        return weight;
    }

    public List<Step> getSteps() {
        return this.steps;
    }
}
