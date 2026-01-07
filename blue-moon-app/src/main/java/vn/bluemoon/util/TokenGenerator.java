package vn.bluemoon.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating secure tokens
 */
public class TokenGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    /**
     * Generate a secure random token
     * @return Base64 encoded token
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate a session token
     * @return Session token string
     */
    public static String generateSessionToken() {
        return generateToken();
    }

    /**
     * Generate a password reset token
     * @return Password reset token string
     */
    public static String generatePasswordResetToken() {
        return generateToken();
    }
}

















