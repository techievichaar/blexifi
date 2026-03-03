package com.blexifi.mesh;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Envelope {
    public final UUID envelopeId;
    public final String sourceId;
    public final String destinationId;
    public final long timestampMs;
    public final int ttl;
    public final int hopCount;
    public final boolean requiresAck;
    public final PayloadType payloadType;
    public final String ciphertext;
    public final UUID ackForEnvelopeId;

    public Envelope(
            UUID envelopeId,
            String sourceId,
            String destinationId,
            long timestampMs,
            int ttl,
            int hopCount,
            boolean requiresAck,
            PayloadType payloadType,
            String ciphertext,
            UUID ackForEnvelopeId
    ) {
        this.envelopeId = Objects.requireNonNull(envelopeId);
        this.sourceId = Objects.requireNonNull(sourceId);
        this.destinationId = Objects.requireNonNull(destinationId);
        this.timestampMs = timestampMs;
        this.ttl = ttl;
        this.hopCount = hopCount;
        this.requiresAck = requiresAck;
        this.payloadType = Objects.requireNonNull(payloadType);
        this.ciphertext = Objects.requireNonNull(ciphertext);
        this.ackForEnvelopeId = ackForEnvelopeId;
    }

    public static Envelope text(String sourceId, String destinationId, String ciphertext, int ttl) {
        return new Envelope(
                UUID.randomUUID(),
                sourceId,
                destinationId,
                Instant.now().toEpochMilli(),
                ttl,
                0,
                true,
                PayloadType.TEXT,
                ciphertext,
                null
        );
    }

    public Envelope relayCopy() {
        return new Envelope(
                envelopeId,
                sourceId,
                destinationId,
                timestampMs,
                ttl - 1,
                hopCount + 1,
                requiresAck,
                payloadType,
                ciphertext,
                ackForEnvelopeId
        );
    }
}
