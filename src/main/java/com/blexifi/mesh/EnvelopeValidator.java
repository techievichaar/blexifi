package com.blexifi.mesh;

public final class EnvelopeValidator {
    private EnvelopeValidator() {
    }

    public static String validate(Envelope envelope) {
        if (envelope.sourceId == null || envelope.sourceId.isBlank()) {
            return "invalid_source";
        }
        if (envelope.destinationId == null || envelope.destinationId.isBlank()) {
            return "invalid_destination";
        }
        if (envelope.ttl < 0) {
            return "invalid_ttl";
        }
        if (envelope.hopCount < 0) {
            return "invalid_hop_count";
        }
        if (envelope.payloadType == PayloadType.ACK && envelope.ackForEnvelopeId == null) {
            return "invalid_ack";
        }
        return null;
    }
}
