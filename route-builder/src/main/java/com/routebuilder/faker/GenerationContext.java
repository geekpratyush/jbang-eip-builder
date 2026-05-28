package com.routebuilder.faker;

import java.util.HashMap;
import java.util.Map;

public class GenerationContext {
    private final Map<String, String> cache = new HashMap<>();
    private final Map<String, Object> objectCache = new HashMap<>();
    private final Map<String, Object> overrides = new HashMap<>();

    public void setOverrides(Map<String, Object> overrides) {
        if (overrides != null) {
            this.overrides.putAll(overrides);
        }
    }

    public String getCachedValue(String key) {
        return cache.get(key);
    }

    public void cacheValue(String key, String value) {
        cache.put(key, value);
    }

    public Object getCachedObject(String key) {
        return objectCache.get(key);
    }

    public void cacheObject(String key, Object obj) {
        objectCache.put(key, obj);
    }

    public Object getOverride(String key) {
        return overrides.get(key);
    }

    public GenerationContext fork() {
        GenerationContext context = new GenerationContext();
        context.overrides.putAll(this.overrides);
        return context;
    }
}
