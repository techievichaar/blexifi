package com.blexifi.mesh;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {
    void upsert(OutboxEntry entry);

    Optional<OutboxEntry> get(UUID envelopeId);

    List<OutboxEntry> due(long nowMs);
}
