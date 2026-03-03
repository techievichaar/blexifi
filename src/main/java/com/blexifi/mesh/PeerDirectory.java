package com.blexifi.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PeerDirectory {
    public record PeerRecord(String peerId, long lastSeenMs, List<String> capabilities) {
    }

    private final Map<String, PeerRecord> peers = new LinkedHashMap<>();

    public synchronized void updateFromBlePayload(BlePresencePayload payload, long nowMs) {
        peers.put(payload.deviceId(), new PeerRecord(payload.deviceId(), nowMs, payload.capabilities()));
    }

    public synchronized List<PeerRecord> listPeersNewestFirst() {
        return peers.values().stream()
                .sorted(Comparator.comparingLong(PeerRecord::lastSeenMs).reversed())
                .toList();
    }

    public synchronized List<PeerLink> toPeerLinks(String excludePeerId, long nowMs) {
        List<PeerLink> links = new ArrayList<>();
        for (PeerRecord peer : peers.values()) {
            if (peer.peerId().equals(excludePeerId)) {
                continue;
            }
            int recency = recencyWeight(peer.lastSeenMs(), nowMs);
            int linkQuality = peer.capabilities().contains("wifi_direct") ? 9 : 5;
            int delivery = peer.capabilities().contains("relay") ? 8 : 4;
            links.add(new PeerLink(peer.peerId(), recency, linkQuality, delivery));
        }
        return links;
    }

    private static int recencyWeight(long lastSeenMs, long nowMs) {
        long ageMs = Math.max(0, nowMs - lastSeenMs);
        if (ageMs < 10_000) return 10;
        if (ageMs < 60_000) return 8;
        if (ageMs < 5 * 60_000) return 6;
        return 3;
    }
}
