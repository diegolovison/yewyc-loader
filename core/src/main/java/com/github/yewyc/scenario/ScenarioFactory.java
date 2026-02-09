package com.github.yewyc.scenario;

import io.netty.channel.EventLoop;

import java.util.function.Function;

public interface ScenarioFactory extends Function<EventLoop, Scenario> {

}