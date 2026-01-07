package vn.bluemoon.ui.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import vn.bluemoon.exception.AuthException;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.LoginRequest;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.service.AuthService;
import vn.bluemoon.util.ErrorDialog;
import vn.bluemoon.validation.ValidationException;

import java.io.IOException;

/**
 * Controller for login view
 */
public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Hyperlink forgotPasswordLink;
    
    @FXML
    private Label errorLabel;
    
    private Stage stage;
    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    private void handleLogin() {
        errorLabel.setVisible(false);
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }
        
        try {
            LoginRequest request = new LoginRequest(username, password);
            String sessionToken = authService.login(request);
            
            // Get current user
            vn.bluemoon.model.entity.User currentUser = authService.getCurrentUser(sessionToken);
            
            // Store session token
            sessionManager.createSession(currentUser);
            
            // Check if user needs to change password
            if (currentUser != null && Boolean.TRUE.equals(currentUser.getMustChangePassword())) {
                // Show change password dialog
                boolean passwordChanged = vn.bluemoon.ui.dialog.ChangePasswordDialog.show(
                    currentUser.getId(), 
                    "Bạn cần đổi mật khẩu. Vui lòng đổi mật khẩu để tiếp tục."
                );
                
                if (!passwordChanged) {
                    // User cancelled or failed to change password, logout
                    authService.logout(sessionToken);
                    showError("Bạn phải đổi mật khẩu để tiếp tục sử dụng hệ thống");
                    return;
                }
                
                // Reload user to get updated info
                currentUser = authService.getCurrentUser(sessionToken);
                if (currentUser != null) {
                    sessionManager.createSession(currentUser);
                }
            }
            
            // Navigate to main application
            navigateToMain(sessionToken);
            
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (AuthException e) {
            showError(e.getMessage());
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/register/RegisterView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 500);
            Stage registerStage = new Stage();
            registerStage.setTitle("Đăng ký");
            registerStage.setScene(scene);
            registerStage.setResizable(false);
            registerStage.show();
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình đăng ký: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Quên mật khẩu");
        dialog.setHeaderText("Nhập email của bạn");
        dialog.setContentText("Email:");
        
        dialog.showAndWait().ifPresent(email -> {
            try {
                vn.bluemoon.service.PasswordResetService service = new vn.bluemoon.service.PasswordResetService();
                service.requestPasswordReset(email);
                ErrorDialog.showInfo("Thành công", "Email đặt lại mật khẩu đã được gửi đến " + email);
            } catch (Exception e) {
                ErrorDialog.showError("Lỗi", "Không thể gửi email đặt lại mật khẩu: " + e.getMessage());
            }
        });
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void navigateToMain(String sessionToken) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main/MainView.fxml"));
            Parent root = loader.load();
            
            // Set main stage cho MainController
            vn.bluemoon.ui.main.MainController mainController = loader.getController();
            if (mainController != null && stage != null) {
                mainController.setMainStage(stage);
            }
            
            Scene scene = new Scene(root, 1200, 800);
            if (stage != null) {
                stage.setTitle("Blue Moon - Hệ thống quản lý chung cư");
                stage.setResizable(true);
                stage.setScene(scene);
                // Đảm bảo reset trước khi set maximized để tránh lỗi
                stage.setMaximized(false);
                // Sử dụng Platform.runLater để đảm bảo setMaximized được gọi sau khi scene đã được set
                javafx.application.Platform.runLater(() -> {
                    stage.setMaximized(true); // Hiển thị toàn màn hình
                });
            }
        } catch (IOException e) {
            ErrorDialog.showError("Lỗi", "Không thể mở màn hình chính: " + e.getMessage());
        }
    }
}

