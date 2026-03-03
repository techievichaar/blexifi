package com.blexifi.mesh;

import java.util.UUID;

public record OutboxEntry(
        UUID envelopeId,
        Envelope envelope,
        int attempt,
        long nextAttemptAtMs,
        boolean acked,
        boolean expired
) {
    public OutboxEntry nextRetry(long nextAttemptAtMs) {
        return new OutboxEntry(envelopeId, envelope, attempt + 1, nextAttemptAtMs, acked, expired);
    }

    public OutboxEntry markAcked() {
        return new OutboxEntry(envelopeId, envelope, attempt, nextAttemptAtMs, true, expired);
    }

    public OutboxEntry markExpired() {
        return new OutboxEntry(envelopeId, envelope, attempt, nextAttemptAtMs, acked, true);
    }
}
