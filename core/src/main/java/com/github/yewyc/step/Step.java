package com.github.yewyc.step;

import com.github.yewyc.scenario.ScenarioContext;
import io.netty.channel.EventLoop;

import java.util.concurrent.CompletionStage;

public abstract class Step {

    public abstract CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id);
}
