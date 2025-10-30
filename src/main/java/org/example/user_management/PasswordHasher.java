package org.example.user_management;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHasher {
    public String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        if (rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] bytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = createDigest();
        byte[] hashed = digest.digest(bytes);
        return toHex(hashed);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        try {
            return hash(rawPassword).equals(hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available.", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
