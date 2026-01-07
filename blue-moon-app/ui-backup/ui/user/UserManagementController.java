package vn.bluemoon.ui.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.UserSearchRequest;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.service.UserService;
import vn.bluemoon.util.ErrorDialog;

import java.util.List;

/**
 * Controller for user management view
 */
public class UserManagementController {
    @FXML
    private TextField searchUsernameField;
    
    @FXML
    private TextField searchEmailField;
    
    @FXML
    private TextField searchFullNameField;
    
    @FXML
    private TextField searchPhoneField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private TableView<User> userTable;
    
    @FXML
    private TableColumn<User, Integer> sttColumn;
    
    @FXML
    private TableColumn<User, String> usernameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> fullNameColumn;
    
    @FXML
    private TableColumn<User, String> phoneColumn;
    
    @FXML
    private TableColumn<User, String> isActiveColumn;
    
    @FXML
    private Button disableButton;
    
    @FXML
    private Button enableButton;
    
    @FXML
    private Button requirePasswordChangeButton;
    
    @FXML
    private Button deleteButton;
    
    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Setup table columns
        sttColumn.setCellValueFactory(param -> {
            int index = userList.indexOf(param.getValue());
            return javafx.beans.binding.Bindings.createObjectBinding(() -> index + 1);
        });
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        isActiveColumn.setCellValueFactory(param -> {
            User user = param.getValue();
            Boolean isActive = user.getIsActive();
            
            // Kiểm tra xem user có resident record không
            try {
                Resident resident = new ResidentRepository().findByUserId(user.getId());
                if (resident == null) {
                    // User chưa đăng ký nơi ở
                    return javafx.beans.binding.Bindings.createStringBinding(() -> 
                        (isActive != null && isActive ? "Hoạt động" : "Vô hiệu hóa") + " - Chưa đăng ký nơi ở"
                    );
                } else {
                    // User đã đăng ký nơi ở
                    return javafx.beans.binding.Bindings.createStringBinding(() -> 
                        isActive != null && isActive ? "Hoạt động" : "Vô hiệu hóa"
                    );
                }
            } catch (DbException e) {
                // Nếu có lỗi, chỉ hiển thị trạng thái active
                return javafx.beans.binding.Bindings.createStringBinding(() -> 
                    isActive != null && isActive ? "Hoạt động" : "Vô hiệu hóa"
                );
            }
        });
        
        userTable.setItems(userList);
        
        // Load all users initially
        handleSearch();
    }
    
    @FXML
    private void handleSearch() {
        try {
            UserSearchRequest request = new UserSearchRequest();
            request.setUsername(searchUsernameField.getText().trim());
            request.setEmail(searchEmailField.getText().trim());
            request.setFullName(searchFullNameField.getText().trim());
            request.setPhone(searchPhoneField.getText().trim());
            
            List<User> users = userService.searchUsers(request);
            userList.clear();
            userList.addAll(users);
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDisable() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn người dùng cần vô hiệu hóa");
            return;
        }
        
        if (ErrorDialog.showConfirmation("Xác nhận", "Bạn có chắc chắn muốn vô hiệu hóa người dùng này?")) {
            try {
                userService.disableUser(selectedUser.getId());
                ErrorDialog.showInfo("Thành công", "Đã vô hiệu hóa người dùng thành công");
                handleSearch();
            } catch (DbException e) {
                ErrorDialog.showDbError(e.getMessage());
            } catch (Exception e) {
                ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleEnable() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn người dùng cần kích hoạt");
            return;
        }
        
        try {
            userService.enableUser(selectedUser.getId());
            ErrorDialog.showInfo("Thành công", "Đã kích hoạt người dùng thành công");
            handleSearch();
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRequirePasswordChange() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn người dùng cần yêu cầu đổi mật khẩu");
            return;
        }
        
        if (ErrorDialog.showConfirmation("Xác nhận", "Bạn có chắc chắn muốn yêu cầu người dùng này đổi mật khẩu?")) {
            try {
                userService.requirePasswordChange(selectedUser.getId());
                ErrorDialog.showInfo("Thành công", "Đã yêu cầu người dùng đổi mật khẩu thành công");
                handleSearch();
            } catch (DbException e) {
                ErrorDialog.showDbError(e.getMessage());
            } catch (Exception e) {
                ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleDelete() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn một người dùng để xóa");
            return;
        }
        
        // Xác nhận xóa
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText("Xóa người dùng");
        confirmDialog.setContentText(
            "Bạn có chắc chắn muốn xóa người dùng này không?\n\n" +
            "Tên đăng nhập: " + selectedUser.getUsername() + "\n" +
            "Họ và tên: " + selectedUser.getFullName() + "\n" +
            "Email: " + selectedUser.getEmail() + "\n\n" +
            "LƯU Ý: Nếu người dùng này là chủ hộ, tất cả thu phí và nhân khẩu liên quan sẽ bị xóa!"
        );
        
        confirmDialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    userService.deleteUser(selectedUser.getId());
                    ErrorDialog.showInfo("Thành công", "Đã xóa người dùng thành công");
                    handleSearch();
                } catch (DbException e) {
                    ErrorDialog.showDbError(e.getMessage());
                } catch (Exception e) {
                    ErrorDialog.showError("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
                }
            }
        });
    }
}








