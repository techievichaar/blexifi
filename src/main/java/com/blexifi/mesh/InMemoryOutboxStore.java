package com.blexifi.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryOutboxStore implements OutboxStore {
    private final Map<UUID, OutboxEntry> entries = new HashMap<>();

    @Override
    public synchronized void upsert(OutboxEntry entry) {
        entries.put(entry.envelopeId(), entry);
    }

    @Override
    public synchronized Optional<OutboxEntry> get(UUID envelopeId) {
        return Optional.ofNullable(entries.get(envelopeId));
    }

    @Override
    public synchronized List<OutboxEntry> due(long nowMs) {
        List<OutboxEntry> list = new ArrayList<>();
        for (OutboxEntry entry : entries.values()) {
            if (!entry.acked() && !entry.expired() && entry.nextAttemptAtMs() <= nowMs) {
                list.add(entry);
            }
        }
        list.sort(Comparator.comparingLong(OutboxEntry::nextAttemptAtMs));
        return list;
    }
}
