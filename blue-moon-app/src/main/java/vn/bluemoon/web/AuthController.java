package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.AuthException;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.LoginRequest;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.service.AuthService;
import vn.bluemoon.validation.ValidationException;

import javax.servlet.http.HttpSession;

/**
 * Web controller for authentication (login, register, logout)
 */
@Controller
public class AuthController {
    
    private final AuthService authService = new AuthService();
    private final SessionManager sessionManager = SessionManager.getInstance();
    
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String loginPage(Model model) {
        return "login";
    }
    
    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            LoginRequest request = new LoginRequest(username, password);
            String sessionToken = authService.login(request);
            
            User currentUser = authService.getCurrentUser(sessionToken);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Đăng nhập thất bại");
                return "redirect:/login";
            }
            
            // Store user in Spring session
            session.setAttribute("user", currentUser);
            session.setAttribute("sessionToken", sessionToken);
            
            // Also store in SessionManager for compatibility
            sessionManager.createSession(currentUser);
            
            // Check if user needs to change password
            if (Boolean.TRUE.equals(currentUser.getMustChangePassword())) {
                redirectAttributes.addFlashAttribute("requirePasswordChange", true);
                redirectAttributes.addFlashAttribute("userId", currentUser.getId());
                return "redirect:/change-password";
            }
            
            return "redirect:/main";
            
        } catch (ValidationException | AuthException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kết nối database: " + e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi: " + e.getMessage());
            return "redirect:/login";
        }
    }
    
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String fullName,
            @RequestParam String phone,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/register";
            }
            
            vn.bluemoon.model.dto.RegisterRequest request = new vn.bluemoon.model.dto.RegisterRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setPassword(password);
            request.setFullName(fullName);
            request.setPhone(phone);
            
            vn.bluemoon.service.UserService userService = new vn.bluemoon.service.UserService();
            userService.register(request);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
            
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (vn.bluemoon.exception.DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kết nối database: " + e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi: " + e.getMessage());
            return "redirect:/register";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String sessionToken = (String) session.getAttribute("sessionToken");
        if (sessionToken != null) {
            authService.logout(sessionToken);
            sessionManager.invalidateSession(sessionToken);
        }
        session.invalidate();
        return "redirect:/login";
    }
    
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        
        try {
            vn.bluemoon.service.PasswordResetService service = new vn.bluemoon.service.PasswordResetService();
            service.requestPasswordReset(email);
            redirectAttributes.addFlashAttribute("success", "Email đặt lại mật khẩu đã được gửi đến " + email);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi email: " + e.getMessage());
        }
        
        return "redirect:/forgot-password";
    }
}

