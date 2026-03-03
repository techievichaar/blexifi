package com.blexifi.mesh;

import java.util.Comparator;
import java.util.List;

public final class MeshRouter {
    private final SeenCache seenCache;
    private final int defaultFanout;

    public MeshRouter(SeenCache seenCache, int defaultFanout) {
        this.seenCache = seenCache;
        this.defaultFanout = defaultFanout;
    }

    public MeshRouter(SeenCache seenCache) {
        this(seenCache, 2);
    }

    public RelayDecision onReceive(
            String selfId,
            Envelope envelope,
            List<PeerLink> neighbors,
            String previousHopPeerId,
            int fanout
    ) {
        if (seenCache.hasSeen(envelope.envelopeId)) {
            return new RelayDecision(false, List.of(), "duplicate");
        }
        seenCache.markSeen(envelope.envelopeId);

        if (envelope.destinationId.equals(selfId)) {
            return new RelayDecision(true, List.of(), null);
        }

        if (envelope.ttl <= 0) {
            return new RelayDecision(false, List.of(), "ttl_expired");
        }

        List<ForwardCommand> forwards = neighbors.stream()
                .filter(link -> !link.peerId().equals(previousHopPeerId))
                .sorted(Comparator.comparingInt(PeerLink::score).reversed())
                .limit(fanout)
                .map(link -> new ForwardCommand(link.peerId(), envelope.relayCopy()))
                .toList();

        if (forwards.isEmpty()) {
            return new RelayDecision(false, List.of(), "no_route");
        }

        return new RelayDecision(false, forwards, null);
    }

    public RelayDecision onReceive(String selfId, Envelope envelope, List<PeerLink> neighbors) {
        return onReceive(selfId, envelope, neighbors, null, defaultFanout);
    }
}
