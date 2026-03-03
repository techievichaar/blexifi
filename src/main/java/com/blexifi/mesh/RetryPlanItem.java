package com.blexifi.mesh;

import java.util.UUID;

public record RetryPlanItem(
        UUID envelopeId,
        int attempts,
        long retryDelayMs,
        long nextAttemptAtMs
) {
}
