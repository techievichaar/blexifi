package com.blexifi.mesh;

import java.util.List;

public record NodeProcessResult(
        boolean deliveredLocally,
        Envelope ackToSend,
        List<ForwardCommand> forwards,
        String dropReason
) {
}
