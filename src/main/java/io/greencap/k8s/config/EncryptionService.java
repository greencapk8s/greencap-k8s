package io.greencap.k8s.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EncryptionService {

    private final TextEncryptor encryptor;

    public EncryptionService(@Value("${greencap.encryption.key}") String encryptionKey) {
        // Hex salt for PBKDF2 key derivation — per-message IV is handled internally by Spring
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexSalt = new StringBuilder();
        for (int i = 0; i < Math.min(8, keyBytes.length); i++) {
            hexSalt.append(String.format("%02x", keyBytes[i]));
        }
        this.encryptor = Encryptors.text(encryptionKey, hexSalt.toString());
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return ciphertext;
        return encryptor.decrypt(ciphertext);
    }
}
