package vn.bluemoon.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import vn.bluemoon.service.UserService;
import vn.bluemoon.util.ErrorDialog;

import java.util.Optional;

/**
 * Dialog để user đổi mật khẩu khi được yêu cầu
 */
public class ChangePasswordDialog {
    
    public static boolean show(Integer userId, String message) {
        // Tạo dialog từ code thay vì FXML để tránh lỗi
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText(message != null && !message.isEmpty() ? message : "Bạn cần đổi mật khẩu");
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        // Tạo layout
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));
        vbox.setPrefWidth(400);
        
        Label messageLabel = new Label("Vui lòng đổi mật khẩu của bạn");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        
        Label newPasswordLabel = new Label("Mật khẩu mới:");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nhập mật khẩu mới");
        
        Label confirmPasswordLabel = new Label("Xác nhận mật khẩu:");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Nhập lại mật khẩu mới");
        
        Label noteLabel = new Label("Lưu ý: Mật khẩu phải có ít nhất 6 ký tự");
        noteLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        vbox.getChildren().addAll(messageLabel, newPasswordLabel, newPasswordField, 
                                  confirmPasswordLabel, confirmPasswordField, noteLabel);
        
        dialog.getDialogPane().setContent(vbox);
        
        // Thêm nút
        ButtonType changeButtonType = new ButtonType("Đổi mật khẩu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, cancelButtonType);
        
        // Xử lý khi click Đổi mật khẩu
        dialog.setResultConverter(buttonType -> {
            if (buttonType == changeButtonType) {
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                
                // Validate
                if (newPassword == null || newPassword.trim().isEmpty()) {
                    ErrorDialog.showError("Lỗi", "Vui lòng nhập mật khẩu mới");
                    return null;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    ErrorDialog.showError("Lỗi", "Mật khẩu xác nhận không khớp");
                    return null;
                }
                
                if (newPassword.length() < 6) {
                    ErrorDialog.showError("Lỗi", "Mật khẩu phải có ít nhất 6 ký tự");
                    return null;
                }
                
                try {
                    UserService userService = new UserService();
                    userService.changePassword(userId, newPassword);
                    ErrorDialog.showInfo("Thành công", "Đã đổi mật khẩu thành công");
                    return buttonType;
                } catch (vn.bluemoon.validation.ValidationException e) {
                    ErrorDialog.showError("Lỗi", e.getMessage());
                    return null;
                } catch (vn.bluemoon.exception.DbException e) {
                    ErrorDialog.showDbError(e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == changeButtonType;
    }
}
