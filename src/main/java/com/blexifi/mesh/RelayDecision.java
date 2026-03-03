package com.blexifi.mesh;

import java.util.List;

public record RelayDecision(boolean deliverLocally, List<ForwardCommand> forwardCommands, String dropReason) {
}
