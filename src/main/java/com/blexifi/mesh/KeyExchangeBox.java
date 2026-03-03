package com.blexifi.mesh;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;

public final class KeyExchangeBox {
    private KeyExchangeBox() {
    }

    public static KeyPair generateIdentityKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("X25519");
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate X25519 key pair", e);
        }
    }

    public static SecretKey deriveAesKey(PrivateKey privateKey, PublicKey remotePublicKey) {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance("X25519");
            agreement.init(privateKey);
            agreement.doPhase(remotePublicKey, true);
            byte[] sharedSecret = agreement.generateSecret();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(sharedSecret);
            return new SecretKeySpec(hashed, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to derive shared AES key", e);
        }
    }
}
