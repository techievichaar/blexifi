package com.blexifi.mesh;

import java.security.MessageDigest;
import java.security.PublicKey;

public final class IdentityFingerprint {
    private IdentityFingerprint() {
    }

    public static String sha256Hex(PublicKey publicKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fingerprint key", e);
        }
    }

    public static String displayCode(PublicKey publicKey) {
        String hex = sha256Hex(publicKey);
        return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12);
    }
}
