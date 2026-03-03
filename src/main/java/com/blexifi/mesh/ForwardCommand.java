package com.blexifi.mesh;

public record ForwardCommand(String nextHopPeerId, Envelope envelope) {
}
