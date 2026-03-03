package com.blexifi.mesh;

import java.util.UUID;

public record StoredMessage(
        UUID envelopeId,
        String sourceId,
        String destinationId,
        String ciphertext,
        int attempts,
        DeliveryState state
) {
    public StoredMessage incrementAttempts() {
        return new StoredMessage(envelopeId, sourceId, destinationId, ciphertext, attempts + 1, state);
    }

    public StoredMessage withState(DeliveryState newState) {
        return new StoredMessage(envelopeId, sourceId, destinationId, ciphertext, attempts, newState);
    }
}
