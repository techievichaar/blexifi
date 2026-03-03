package com.blexifi.mesh;

public final class KeyRotationPolicy {
    private final long rotationIntervalMs;

    public KeyRotationPolicy(long rotationIntervalMs) {
        this.rotationIntervalMs = rotationIntervalMs;
    }

    public boolean shouldRotate(long lastRotatedAtMs, long nowMs) {
        return (nowMs - lastRotatedAtMs) >= rotationIntervalMs;
    }
}
