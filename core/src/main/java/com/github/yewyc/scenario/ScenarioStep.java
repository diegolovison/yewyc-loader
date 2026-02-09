package com.github.yewyc.scenario;

import io.netty.channel.EventLoop;

import java.util.concurrent.CompletionStage;

public interface ScenarioStep {
    CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id);
}