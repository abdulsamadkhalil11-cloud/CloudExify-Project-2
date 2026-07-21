package com.library.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBKDF2WithHmacSHA256 password hashing. Every stored password/security-answer
 * is (salt, hash) — never plaintext, never a reversible encryption.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordUtil() {
    }

    /** Generates a fresh random salt, Base64-encoded for storage as TEXT. */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes {@code plainText} with the given Base64 salt. Deterministic for the same inputs. */
    public static String hash(String plainText, String base64Salt) {
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            PBEKeySpec spec = new PBEKeySpec(plainText.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashed = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    /** Convenience: generates a new salt and hashes in one call, for account creation. */
    public static String[] hashNew(String plainText) {
        String salt = generateSalt();
        return new String[] { hash(plainText, salt), salt };
    }

    /** Constant-time comparison so verification timing can't leak how much of the hash matched. */
    public static boolean verify(String plainText, String base64Salt, String expectedHash) {
        if (plainText == null || base64Salt == null || expectedHash == null) {
            return false;
        }
        String actualHash = hash(plainText, base64Salt);
        byte[] a = actualHash.getBytes();
        byte[] b = expectedHash.getBytes();
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
