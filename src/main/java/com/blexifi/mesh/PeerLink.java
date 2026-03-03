package com.blexifi.mesh;

public record PeerLink(
        String peerId,
        int recencyWeight,
        int linkQualityWeight,
        int historicalDeliveryWeight
) {
    public int score() {
        return recencyWeight + linkQualityWeight + historicalDeliveryWeight;
    }
}
