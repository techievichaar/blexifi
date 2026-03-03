package com.blexifi.mesh;

public final class RetryBackoffPolicy {
    private final long baseDelayMs;
    private final long maxDelayMs;

    public RetryBackoffPolicy(long baseDelayMs, long maxDelayMs) {
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    public long delayForAttempt(int attempts) {
        if (attempts <= 0) {
            return baseDelayMs;
        }

        long delay = baseDelayMs;
        for (int i = 1; i < attempts; i++) {
            if (delay >= maxDelayMs / 2) {
                return maxDelayMs;
            }
            delay *= 2;
        }

        return Math.min(delay, maxDelayMs);
    }
}
