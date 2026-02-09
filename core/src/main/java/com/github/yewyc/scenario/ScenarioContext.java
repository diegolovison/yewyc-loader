package com.github.yewyc.scenario;

import java.util.HashMap;
import java.util.Map;

public class ScenarioContext {
    private final Map<String, Object> data = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) data.get(key); }
    public void put(String key, Object value) { data.put(key, value); }
}
