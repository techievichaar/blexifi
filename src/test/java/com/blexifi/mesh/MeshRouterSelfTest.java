package com.blexifi.mesh;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;

public final class MeshRouterSelfTest {
    public static void main(String[] args) {
        deliversLocallyWhenDestinationIsSelf();
        forwardsToTopScoringNeighborsAndDecrementsTtl();
        dropsDuplicateEnvelopes();
        simulatesAToBThroughCandDRelayChain();
        simulatesAckRelayBackToSource();
        encryptsAndDecryptsPayload();
        deliveryManagerMarksDeliveredWhenAckArrives();
        deliveryManagerFailsAfterMaxAttempts();
        retryBackoffPolicyUsesExponentialDelay();
        deliveryManagerBuildsRetryPlanForPendingMessages();
        fileMessageStorePersistsAndLoadsMessages();
        rejectsInvalidEnvelopeBeforeRouting();
        derivesMatchingSessionKeysViaX25519();
        System.out.println("MeshRouterSelfTest: ALL TESTS PASSED");
    }

    private static void deliversLocallyWhenDestinationIsSelf() {
        MeshRouter router = new MeshRouter(new SeenCache());
        Envelope envelope = Envelope.text("A", "B", "enc", 6);

        RelayDecision decision = router.onReceive("B", envelope, List.of(), "A", 2);

        check(decision.deliverLocally(), "destination self should deliver locally");
        check(decision.forwardCommands().isEmpty(), "destination self should not forward");
    }

    private static void forwardsToTopScoringNeighborsAndDecrementsTtl() {
        MeshRouter router = new MeshRouter(new SeenCache());
        Envelope envelope = Envelope.text("A", "B", "enc", 4);

        RelayDecision decision = router.onReceive(
                "C",
                envelope,
                List.of(
                        new PeerLink("A", 5, 5, 5),
                        new PeerLink("D", 9, 9, 7),
                        new PeerLink("E", 8, 7, 7)
                ),
                "A",
                2
        );

        check(!decision.deliverLocally(), "relay node should not deliver");
        check(decision.forwardCommands().size() == 2, "should forward to top two peers");
        check(decision.forwardCommands().get(0).nextHopPeerId().equals("D"), "first next hop should be D");
        check(decision.forwardCommands().get(1).nextHopPeerId().equals("E"), "second next hop should be E");
        check(decision.forwardCommands().get(0).envelope().ttl == 3, "ttl should decrement");
        check(decision.forwardCommands().get(0).envelope().hopCount == 1, "hop count should increment");
    }

    private static void dropsDuplicateEnvelopes() {
        MeshRouter router = new MeshRouter(new SeenCache());
        Envelope envelope = Envelope.text("A", "B", "enc", 6);

        RelayDecision first = router.onReceive("C", envelope, List.of());
        RelayDecision second = router.onReceive("C", envelope, List.of());

        check("no_route".equals(first.dropReason()), "first should fail due to no route");
        check("duplicate".equals(second.dropReason()), "second should be dropped as duplicate");
    }

    private static void simulatesAToBThroughCandDRelayChain() {
        MeshRouter cRouter = new MeshRouter(new SeenCache());
        MeshRouter dRouter = new MeshRouter(new SeenCache());
        MeshRouter bRouter = new MeshRouter(new SeenCache());

        Envelope start = Envelope.text("A", "B", "enc", 6);

        RelayDecision atC = cRouter.onReceive(
                "C",
                start,
                List.of(new PeerLink("D", 9, 8, 8)),
                "A",
                2
        );

        Envelope toD = atC.forwardCommands().get(0).envelope();
        RelayDecision atD = dRouter.onReceive(
                "D",
                toD,
                List.of(new PeerLink("B", 9, 9, 9)),
                "C",
                2
        );

        Envelope toB = atD.forwardCommands().get(0).envelope();
        RelayDecision atB = bRouter.onReceive("B", toB, List.of(), "D", 2);

        check(atB.deliverLocally(), "B should receive envelope");
        check(atB.forwardCommands().isEmpty(), "B should not forward after delivery");
    }

    private static void simulatesAckRelayBackToSource() {
        NodeProcessor bNode = new NodeProcessor("B", new MeshRouter(new SeenCache()));
        NodeProcessor dNode = new NodeProcessor("D", new MeshRouter(new SeenCache()));
        NodeProcessor cNode = new NodeProcessor("C", new MeshRouter(new SeenCache()));
        NodeProcessor aNode = new NodeProcessor("A", new MeshRouter(new SeenCache()));

        Envelope original = Envelope.text("A", "B", "cipher", 6);

        NodeProcessResult atB = bNode.onIncoming(original, List.of(), "D");
        check(atB.deliveredLocally(), "B should deliver original message");
        check(atB.ackToSend() != null, "B should create ack for source");
        Envelope ack = atB.ackToSend();
        check(ack.isAck(), "generated envelope should be ack");

        NodeProcessResult atD = dNode.onIncoming(ack, List.of(new PeerLink("C", 9, 9, 9)), "B");
        check(atD.forwards().size() == 1, "D should relay ack to C");

        Envelope ackToC = atD.forwards().get(0).envelope();
        NodeProcessResult atC = cNode.onIncoming(ackToC, List.of(new PeerLink("A", 9, 9, 9)), "D");
        check(atC.forwards().size() == 1, "C should relay ack to A");

        Envelope ackToA = atC.forwards().get(0).envelope();
        NodeProcessResult atA = aNode.onIncoming(ackToA, List.of(), "C");
        check(atA.deliveredLocally(), "A should receive ack");
        check(atA.ackToSend() == null, "Ack should not trigger another ack");
    }

    private static void encryptsAndDecryptsPayload() {
        SecretKey key = CryptoBox.generateKey();
        String plaintext = "hello offline mesh";

        String encrypted = CryptoBox.encryptToBase64(key, plaintext);
        String decrypted = CryptoBox.decryptFromBase64(key, encrypted);

        check(!encrypted.equals(plaintext), "ciphertext should differ from plaintext");
        check(plaintext.equals(decrypted), "decryption should restore plaintext");
    }


    private static void deliveryManagerMarksDeliveredWhenAckArrives() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        DeliveryManager manager = new DeliveryManager(store, 3);

        Envelope outgoing = Envelope.text("A", "B", "cipher", 6);
        manager.onOutgoingCreated(outgoing);
        StoredMessage relayed = manager.onForwardAttempt(outgoing.envelopeId);
        check(relayed.state() == DeliveryState.RELAYED, "message should be relayed after attempt");

        Envelope ack = Envelope.ack("B", "A", outgoing.envelopeId, 6);
        manager.onAckReceived(ack);

        StoredMessage delivered = store.findByEnvelopeId(outgoing.envelopeId).orElseThrow();
        check(delivered.state() == DeliveryState.DELIVERED, "ack should mark message delivered");
    }

    private static void deliveryManagerFailsAfterMaxAttempts() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        DeliveryManager manager = new DeliveryManager(store, 2);

        Envelope outgoing = Envelope.text("A", "Z", "cipher", 6);
        manager.onOutgoingCreated(outgoing);

        StoredMessage first = manager.onForwardAttempt(outgoing.envelopeId);
        check(first.state() == DeliveryState.RELAYED, "first attempt should be relayed state");

        StoredMessage second = manager.onForwardAttempt(outgoing.envelopeId);
        check(second.state() == DeliveryState.FAILED, "max attempts should mark failed");
    }

    private static void retryBackoffPolicyUsesExponentialDelay() {
        RetryBackoffPolicy policy = new RetryBackoffPolicy(1_000, 8_000);
        check(policy.delayForAttempt(1) == 1_000, "attempt1 should be base delay");
        check(policy.delayForAttempt(2) == 2_000, "attempt2 should double");
        check(policy.delayForAttempt(3) == 4_000, "attempt3 should double again");
        check(policy.delayForAttempt(4) == 8_000, "attempt4 should cap at max");
        check(policy.delayForAttempt(8) == 8_000, "later attempts should remain capped");
    }

    private static void deliveryManagerBuildsRetryPlanForPendingMessages() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        DeliveryManager manager = new DeliveryManager(store, 5, new RetryBackoffPolicy(1_000, 10_000));

        Envelope one = Envelope.text("A", "B", "c1", 6);
        Envelope two = Envelope.text("A", "C", "c2", 6);

        manager.onOutgoingCreated(one);
        manager.onOutgoingCreated(two);
        manager.onForwardAttempt(one.envelopeId); // one attempt => next should use attempt2 delay

        long now = 100_000;
        var plan = manager.buildRetryPlan(now);

        check(plan.size() == 2, "retry plan should include two pending messages");

        RetryPlanItem first = plan.stream().filter(p -> p.envelopeId().equals(one.envelopeId)).findFirst().orElseThrow();
        RetryPlanItem second = plan.stream().filter(p -> p.envelopeId().equals(two.envelopeId)).findFirst().orElseThrow();

        check(first.retryDelayMs() == 2_000, "attempted message should get attempt2 delay");
        check(second.retryDelayMs() == 1_000, "fresh message should get attempt1 delay");
        check(first.nextAttemptAtMs() == now + 2_000, "next attempt time should include computed delay");
        check(second.nextAttemptAtMs() == now + 1_000, "next attempt time should include computed delay");
    }

    private static void fileMessageStorePersistsAndLoadsMessages() {
        try {
            Path temp = Files.createTempFile("blexifi-store", ".db");

            FileMessageStore store = new FileMessageStore(temp);
            Envelope one = Envelope.text("A", "B", "cipher-one", 6);
            Envelope two = Envelope.text("A", "C", "cipher-two", 6);

            store.upsert(new StoredMessage(one.envelopeId, one.sourceId, one.destinationId, one.ciphertext, 0, DeliveryState.PENDING));
            store.upsert(new StoredMessage(two.envelopeId, two.sourceId, two.destinationId, two.ciphertext, 1, DeliveryState.RELAYED));

            FileMessageStore reopened = new FileMessageStore(temp);
            check(reopened.findByEnvelopeId(one.envelopeId).isPresent(), "persisted message should be reloadable");
            check(reopened.listPending().size() == 2, "pending list should include both persisted pending/relayed messages");

            reopened.upsert(new StoredMessage(one.envelopeId, one.sourceId, one.destinationId, one.ciphertext, 2, DeliveryState.DELIVERED));
            check(reopened.listPending().size() == 1, "delivered message should not stay in pending list");

            Files.deleteIfExists(temp);
        } catch (Exception e) {
            throw new IllegalStateException("fileMessageStorePersistsAndLoadsMessages failed", e);
        }
    }

    private static void rejectsInvalidEnvelopeBeforeRouting() {
        MeshRouter router = new MeshRouter(new SeenCache());

        Envelope invalid = new Envelope(
                java.util.UUID.randomUUID(),
                "",
                "B",
                System.currentTimeMillis(),
                6,
                0,
                true,
                PayloadType.TEXT,
                "cipher",
                null
        );

        RelayDecision decision = router.onReceive("C", invalid, List.of(new PeerLink("D", 1, 1, 1)), "A", 2);
        check("invalid_source".equals(decision.dropReason()), "invalid envelopes should be rejected early");
        check(decision.forwardCommands().isEmpty(), "invalid envelopes should not be forwarded");
    }

    private static void derivesMatchingSessionKeysViaX25519() {
        KeyPair alice = KeyExchangeBox.generateIdentityKeyPair();
        KeyPair bob = KeyExchangeBox.generateIdentityKeyPair();

        var aliceSession = KeyExchangeBox.deriveAesKey(alice.getPrivate(), bob.getPublic());
        var bobSession = KeyExchangeBox.deriveAesKey(bob.getPrivate(), alice.getPublic());

        check(java.util.Arrays.equals(aliceSession.getEncoded(), bobSession.getEncoded()),
                "derived session keys should match on both peers");

        String plaintext = "mesh secret message";
        String encrypted = CryptoBox.encryptToBase64(aliceSession, plaintext);
        String decrypted = CryptoBox.decryptFromBase64(bobSession, encrypted);
        check(plaintext.equals(decrypted), "derived session key should decrypt peer ciphertext");
    }
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Test failed: " + message);
        }
    }
}
