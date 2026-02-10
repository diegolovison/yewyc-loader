package com.github.yewyc.scenario;

import io.netty.channel.EventLoop;

import java.util.concurrent.CompletionStage;

public abstract class ScenarioStep {

    private double weight;

    public ScenarioStep(double weight) {
        this.weight = weight;
    }

    public abstract CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id);

    public double getWeight() {
        return weight;
    }
}