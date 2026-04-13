package com.culinarycoach.security.mfa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    @Test
    void encryptDecrypt_roundTrip() {
        String key = "test-encryption-key-32-characters!";
        String plaintext = "JBSWY3DPEHPK3PXP";

        String encrypted = EncryptionUtil.encrypt(plaintext, key);
        assertNotEquals(plaintext, encrypted);

        String decrypted = EncryptionUtil.decrypt(encrypted, key);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptDecrypt_differentKeys_fails() {
        String key1 = "key-one-32-characters-padding!!!";
        String key2 = "key-two-32-characters-padding!!!";
        String plaintext = "secret-totp-seed";

        String encrypted = EncryptionUtil.encrypt(plaintext, key1);

        assertThrows(RuntimeException.class, () -> {
            EncryptionUtil.decrypt(encrypted, key2);
        });
    }

    @Test
    void encrypt_differentCiphertextsForSamePlaintext() {
        String key = "test-key-for-randomness-check!!";
        String plaintext = "same-input";

        String enc1 = EncryptionUtil.encrypt(plaintext, key);
        String enc2 = EncryptionUtil.encrypt(plaintext, key);

        // Due to random IV, same plaintext should produce different ciphertexts
        assertNotEquals(enc1, enc2);

        // But both should decrypt to the same value
        assertEquals(plaintext, EncryptionUtil.decrypt(enc1, key));
        assertEquals(plaintext, EncryptionUtil.decrypt(enc2, key));
    }
}
