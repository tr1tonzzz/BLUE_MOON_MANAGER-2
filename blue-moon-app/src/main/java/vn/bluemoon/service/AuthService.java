package vn.bluemoon.service;

import vn.bluemoon.exception.AuthException;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.LoginRequest;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.UserRepository;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.util.PasswordHasher;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

/**
 * Authentication service
 */
public class AuthService {
    private final UserRepository userRepository = new UserRepository();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final PasswordChangeService passwordChangeService = new PasswordChangeService();

    /**
     * Login user
     * @param request Login request
     * @return Session token
     * @throws AuthException if login fails
     * @throws DbException if database error occurs
     * @throws ValidationException if validation fails
     */
    public String login(LoginRequest request) throws AuthException, DbException, ValidationException {
        User user;
        
        if (request.isFacebookLogin()) {
            // Facebook login
            user = userRepository.findByFacebookId(request.getFacebookId());
            if (user == null) {
                throw new AuthException("Tài khoản Facebook chưa được đăng ký trong hệ thống");
            }
        } else {
            // Username/password login
            Validators.validateRequired(request.getUsername(), "Tên đăng nhập");
            Validators.validateRequired(request.getPassword(), "Mật khẩu");
            
            user = userRepository.findByUsername(request.getUsername());
            if (user == null) {
                throw new AuthException("Tên đăng nhập hoặc mật khẩu không đúng");
            }
            
            if (!PasswordHasher.verify(request.getPassword(), user.getPasswordHash())) {
                throw new AuthException("Tên đăng nhập hoặc mật khẩu không đúng");
            }
        }
        
        // Check if user is active
        if (!user.getIsActive()) {
            throw new AuthException("Tài khoản đã bị vô hiệu hóa");
        }
        
        // Check if user needs to change password
        if (passwordChangeService.needsPasswordChange(user)) {
            // Set flag để UI biết cần đổi mật khẩu
            user.setMustChangePassword(true);
        }
        
        // Create session
        return sessionManager.createSession(user);
    }

    /**
     * Get current user from session
     * @param sessionToken Session token
     * @return User if session is valid, null otherwise
     */
    public User getCurrentUser(String sessionToken) {
        return sessionManager.getUser(sessionToken);
    }

    /**
     * Logout user
     * @param sessionToken Session token
     */
    public void logout(String sessionToken) {
        sessionManager.invalidateSession(sessionToken);
    }
}


