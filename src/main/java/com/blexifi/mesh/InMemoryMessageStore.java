package com.blexifi.mesh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryMessageStore implements MessageStore {
    private final Map<UUID, StoredMessage> messages = new LinkedHashMap<>();

    @Override
    public synchronized void upsert(StoredMessage message) {
        messages.put(message.envelopeId(), message);
    }

    @Override
    public synchronized Optional<StoredMessage> findByEnvelopeId(UUID envelopeId) {
        return Optional.ofNullable(messages.get(envelopeId));
    }

    @Override
    public synchronized List<StoredMessage> listPending() {
        List<StoredMessage> pending = new ArrayList<>();
        for (StoredMessage message : messages.values()) {
            if (message.state() == DeliveryState.PENDING || message.state() == DeliveryState.RELAYED) {
                pending.add(message);
            }
        }
        return pending;
    }
}
