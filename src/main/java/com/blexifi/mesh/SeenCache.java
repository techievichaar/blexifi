package com.blexifi.mesh;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SeenCache {
    private final Clock clock;
    private final long ttlMs;
    private final Map<UUID, Long> seenAt = new LinkedHashMap<>();

    public SeenCache() {
        this(Clock.systemUTC(), 5 * 60_000L);
    }

    public SeenCache(Clock clock, long ttlMs) {
        this.clock = clock;
        this.ttlMs = ttlMs;
    }

    public void markSeen(UUID id) {
        evictExpired();
        seenAt.put(id, clock.millis());
    }

    public boolean hasSeen(UUID id) {
        evictExpired();
        return seenAt.containsKey(id);
    }

    private void evictExpired() {
        long now = clock.millis();
        var iterator = seenAt.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if ((now - entry.getValue()) > ttlMs) {
                iterator.remove();
            }
        }
    }
}
