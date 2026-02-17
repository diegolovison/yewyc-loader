package com.github.yewyc.step;

import com.github.yewyc.scenario.ScenarioContext;
import io.netty.channel.EventLoop;

import java.util.concurrent.CompletionStage;

public abstract class Step {

    private final String name;

    protected Step(String name) {
        if (name == null) {
            name = "default";
        }
        this.name = name;
    }

    public abstract CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id);

    public String getName() {
        return this.name;
    }
}
