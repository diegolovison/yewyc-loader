package com.github.yewyc.scenario;

import io.netty.channel.EventLoop;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ScenarioContext {

    private final Map<String, Object> data = new HashMap<>();
    private final java.util.Random random;
    private final EventLoop eventLoop;

    public ScenarioContext(EventLoop eventLoop, long id) {
        this.random = new java.util.Random(id);
        this.eventLoop = eventLoop;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) data.get(key); }
    public void put(String key, Object value) { data.put(key, value); }

    public Random getRandom() {
        assert this.eventLoop.inEventLoop();
        return random;
    }
}
