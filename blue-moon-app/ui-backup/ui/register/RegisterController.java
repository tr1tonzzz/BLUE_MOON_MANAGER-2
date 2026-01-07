package vn.bluemoon.ui.register;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.RegisterRequest;
import vn.bluemoon.service.UserService;
import vn.bluemoon.util.ErrorDialog;
import vn.bluemoon.validation.ValidationException;

/**
 * Controller for register view
 */
public class RegisterController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextArea addressField;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService = new UserService();
    
    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        
        // Validate passwords match
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("Mật khẩu và xác nhận mật khẩu không khớp");
            return;
        }
        
        try {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(usernameField.getText().trim());
            request.setEmail(emailField.getText().trim());
            request.setPassword(passwordField.getText());
            request.setFullName(fullNameField.getText().trim());
            request.setPhone(phoneField.getText().trim());
            request.setAddress(addressField.getText().trim());
            
            userService.register(request);
            
            ErrorDialog.showInfo("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
            handleCancel();
            
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}












