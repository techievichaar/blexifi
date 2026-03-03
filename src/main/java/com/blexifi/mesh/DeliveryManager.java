package com.blexifi.mesh;

import java.util.List;
import java.util.UUID;

public final class DeliveryManager {
    private final MessageStore messageStore;
    private final int maxAttempts;
    private final RetryBackoffPolicy retryBackoffPolicy;

    public DeliveryManager(MessageStore messageStore, int maxAttempts) {
        this(messageStore, maxAttempts, new RetryBackoffPolicy(1_000, 60_000));
    }

    public DeliveryManager(MessageStore messageStore, int maxAttempts, RetryBackoffPolicy retryBackoffPolicy) {
        this.messageStore = messageStore;
        this.maxAttempts = maxAttempts;
        this.retryBackoffPolicy = retryBackoffPolicy;
    }

    public StoredMessage onOutgoingCreated(Envelope envelope) {
        StoredMessage stored = new StoredMessage(
                envelope.envelopeId,
                envelope.sourceId,
                envelope.destinationId,
                envelope.ciphertext,
                0,
                DeliveryState.PENDING
        );
        messageStore.upsert(stored);
        return stored;
    }

    public StoredMessage onForwardAttempt(UUID envelopeId) {
        StoredMessage current = messageStore.findByEnvelopeId(envelopeId)
                .orElseThrow(() -> new IllegalStateException("Missing envelope: " + envelopeId));

        StoredMessage updated = current.incrementAttempts();
        if (updated.attempts() >= maxAttempts && updated.state() != DeliveryState.DELIVERED) {
            updated = updated.withState(DeliveryState.FAILED);
        } else if (updated.state() != DeliveryState.DELIVERED) {
            updated = updated.withState(DeliveryState.RELAYED);
        }

        messageStore.upsert(updated);
        return updated;
    }

    public void onAckReceived(Envelope ackEnvelope) {
        if (!ackEnvelope.isAck() || ackEnvelope.ackForEnvelopeId == null) {
            return;
        }

        messageStore.findByEnvelopeId(ackEnvelope.ackForEnvelopeId)
                .map(msg -> msg.withState(DeliveryState.DELIVERED))
                .ifPresent(messageStore::upsert);
    }

    public List<StoredMessage> pendingMessages() {
        return messageStore.listPending();
    }

    public List<RetryPlanItem> buildRetryPlan(long nowMs) {
        return messageStore.listPending().stream()
                .map(message -> {
                    long delay = retryBackoffPolicy.delayForAttempt(message.attempts() + 1);
                    return new RetryPlanItem(message.envelopeId(), message.attempts(), delay, nowMs + delay);
                })
                .toList();
    }
}
