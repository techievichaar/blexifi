package com.blexifi.mesh;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public final class DeliveryCoordinator {
    private static final int MAX_RETRY = 5;
    private final OutboxStore outboxStore;
    private final Clock clock;

    public DeliveryCoordinator(OutboxStore outboxStore, Clock clock) {
        this.outboxStore = outboxStore;
        this.clock = clock;
    }

    public OutboxEntry trackForDelivery(Envelope envelope) {
        OutboxEntry entry = new OutboxEntry(
                envelope.envelopeId,
                envelope,
                0,
                clock.millis(),
                false,
                false
        );
        outboxStore.upsert(entry);
        return entry;
    }

    public List<OutboxEntry> dueRetries() {
        return outboxStore.due(clock.millis());
    }

    public OutboxEntry markAttemptScheduled(UUID envelopeId) {
        OutboxEntry current = outboxStore.get(envelopeId)
                .orElseThrow(() -> new IllegalStateException("Unknown envelope in outbox: " + envelopeId));

        if (current.attempt() >= MAX_RETRY) {
            OutboxEntry expired = current.markExpired();
            outboxStore.upsert(expired);
            return expired;
        }

        long backoffMs = (long) Math.pow(2, current.attempt()) * 1000L;
        OutboxEntry next = current.nextRetry(clock.millis() + backoffMs);
        outboxStore.upsert(next);
        return next;
    }

    public void markAcked(UUID ackForEnvelopeId) {
        outboxStore.get(ackForEnvelopeId)
                .map(OutboxEntry::markAcked)
                .ifPresent(outboxStore::upsert);
    }
}
