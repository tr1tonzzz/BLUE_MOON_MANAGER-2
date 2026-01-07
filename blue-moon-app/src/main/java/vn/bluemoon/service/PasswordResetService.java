package vn.bluemoon.service;

import vn.bluemoon.config.AppConfig;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.PasswordResetToken;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.TokenRepository;
import vn.bluemoon.repository.UserRepository;
import vn.bluemoon.util.EmailSender;
import vn.bluemoon.util.PasswordHasher;
import vn.bluemoon.util.TokenGenerator;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.time.LocalDateTime;

/**
 * Password reset service
 */
public class PasswordResetService {
    private final UserRepository userRepository = new UserRepository();
    private final TokenRepository tokenRepository = new TokenRepository();
    private final AppConfig appConfig = AppConfig.getInstance();

    /**
     * Request password reset
     * @param email User email
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public void requestPasswordReset(String email) throws ValidationException, DbException {
        Validators.validateEmail(email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            // Don't reveal that email doesn't exist for security
            return;
        }

        // Generate token
        String token = TokenGenerator.generatePasswordResetToken();
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusHours(appConfig.getPasswordResetTokenExpiryHours());

        // Create token record
        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token, expiresAt);
        tokenRepository.create(resetToken);

        // Send email
        EmailSender.sendPasswordResetEmail(user.getEmail(), token);
    }

    /**
     * Reset password using token
     * @param token Reset token
     * @param newPassword New password
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public void resetPassword(String token, String newPassword) throws ValidationException, DbException {
        Validators.validatePassword(newPassword);

        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null) {
            throw new ValidationException("Token không hợp lệ");
        }

        if (resetToken.getUsed()) {
            throw new ValidationException("Token đã được sử dụng");
        }

        if (resetToken.isExpired()) {
            throw new ValidationException("Token đã hết hạn");
        }

        // Update password
        User user = userRepository.findById(resetToken.getUserId());
        if (user != null) {
            user.setPasswordHash(PasswordHasher.hash(newPassword));
            user.setMustChangePassword(false);
            userRepository.update(user);
        }

        // Mark token as used
        tokenRepository.markAsUsed(token);
    }
}

















