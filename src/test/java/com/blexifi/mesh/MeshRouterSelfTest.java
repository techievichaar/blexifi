package com.blexifi.mesh;

import javax.crypto.SecretKey;
import java.util.List;

public final class MeshRouterSelfTest {
    public static void main(String[] args) {
        deliversLocallyWhenDestinationIsSelf();
        forwardsToTopScoringNeighborsAndDecrementsTtl();
        dropsDuplicateEnvelopes();
        simulatesAToBThroughCandDRelayChain();
        simulatesAckRelayBackToSource();
        encryptsAndDecryptsPayload();
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Test failed: " + message);
        }
    }
}
