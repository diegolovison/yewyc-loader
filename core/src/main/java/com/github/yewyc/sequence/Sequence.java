package com.github.yewyc.sequence;

import com.github.yewyc.scenario.ScenarioContext;
import com.github.yewyc.step.Step;

import java.util.List;

public class Sequence {

    private final double weight;
    private final List<Step> steps;
    private final Sequence next;

    public Sequence(double weight, List<Step> steps) {
        this(weight, steps, null);
    }

    public Sequence(double weight, List<Step> steps, Sequence next) {
        this.weight = weight;
        this.steps = steps;
        this.next = next;
    }

    public double getWeight() {
        return weight;
    }

    public List<Step> getSteps() {
        return this.steps;
    }

    public boolean isOk(ScenarioContext ctx) {
        return true;
    }

    public Sequence getNext() {
        return next;
    }
}
