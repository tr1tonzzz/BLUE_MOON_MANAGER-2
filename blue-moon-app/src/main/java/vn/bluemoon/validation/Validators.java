package vn.bluemoon.validation;

import java.util.regex.Pattern;

/**
 * Validation utility class
 */
public class Validators {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    /**
     * Validate email format
     * @param email Email to validate
     * @throws ValidationException if email is invalid
     */
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Email không đúng định dạng");
        }
    }

    /**
     * Validate required field
     * @param value Field value
     * @param fieldName Field name for error message
     * @throws ValidationException if field is empty
     */
    public static void validateRequired(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống");
        }
    }

    /**
     * Validate password strength
     * @param password Password to validate
     * @throws ValidationException if password is too weak
     */
    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }

    /**
     * Validate username
     * @param username Username to validate
     * @throws ValidationException if username is invalid
     */
    public static void validateUsername(String username) throws ValidationException {
        validateRequired(username, "Tên đăng nhập");
        if (username.length() < 3) {
            throw new ValidationException("Tên đăng nhập phải có ít nhất 3 ký tự");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới");
        }
    }
}

















