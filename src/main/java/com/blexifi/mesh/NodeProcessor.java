package com.blexifi.mesh;

import java.util.List;

public final class NodeProcessor {
    private final String selfId;
    private final MeshRouter router;

    public NodeProcessor(String selfId, MeshRouter router) {
        this.selfId = selfId;
        this.router = router;
    }

    public NodeProcessResult onIncoming(Envelope incoming, List<PeerLink> neighbors, String previousHopPeerId) {
        RelayDecision decision = router.onReceive(selfId, incoming, neighbors, previousHopPeerId, 2);

        if (!decision.deliverLocally()) {
            return new NodeProcessResult(false, null, decision.forwardCommands(), decision.dropReason());
        }

        Envelope ack = null;
        if (incoming.requiresAck && incoming.payloadType == PayloadType.TEXT) {
            ack = Envelope.ack(selfId, incoming.sourceId, incoming.envelopeId, 6);
        }

        return new NodeProcessResult(true, ack, List.of(), null);
    }
}
