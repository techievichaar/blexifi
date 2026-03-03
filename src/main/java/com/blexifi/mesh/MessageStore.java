package com.blexifi.mesh;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageStore {
    void upsert(StoredMessage message);

    Optional<StoredMessage> findByEnvelopeId(UUID envelopeId);

    List<StoredMessage> listPending();
}
