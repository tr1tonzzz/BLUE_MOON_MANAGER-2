package vn.bluemoon.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing using BCrypt
 */
public class PasswordHasher {
    
    /**
     * Hash a password
     * @param password Plain text password
     * @return Hashed password
     */
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /**
     * Verify a password against a hash
     * @param password Plain text password
     * @param hash Hashed password
     * @return true if password matches hash
     */
    public static boolean verify(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }
}

















