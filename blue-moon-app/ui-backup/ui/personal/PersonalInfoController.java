package vn.bluemoon.ui.personal;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.PersonalInfoRequest;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.service.PersonalInfoService;
import vn.bluemoon.util.ErrorDialog;

/**
 * Controller for Personal Information management
 */
public class PersonalInfoController {
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField idCardField;
    @FXML
    private DatePicker dateOfBirthPicker;
    @FXML
    private ComboBox<String> genderComboBox;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField occupationField;
    @FXML
    private TextArea permanentAddressArea;
    @FXML
    private TextArea temporaryAddressArea;
    @FXML
    private TextField apartmentCodeField;
    @FXML
    private TextField householdCodeField;
    @FXML
    private ComboBox<String> relationshipComboBox;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    
    private PersonalInfoService personalInfoService = new PersonalInfoService();
    private boolean isEditMode = false;
    
    @FXML
    public void initialize() {
        // Setup gender combo box
        genderComboBox.getItems().addAll("Nam", "Nữ");
        
        // Setup relationship combo box - CHỈ CHO PHÉP "Chủ hộ"
        relationshipComboBox.getItems().addAll("Chủ hộ");
        relationshipComboBox.setValue("Chủ hộ");
        relationshipComboBox.setDisable(true); // Không cho phép thay đổi
        
        // Setup status combo box
        statusComboBox.getItems().addAll("Đang ở", "Tạm trú", "Tạm vắng");
        statusComboBox.setValue("Đang ở"); // Mặc định
        
        // Load existing data if available
        loadPersonalInfo();
    }
    
    private void loadPersonalInfo() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                ErrorDialog.showError("Lỗi", "Bạn chưa đăng nhập");
                closeWindow();
                return;
            }
            
            Resident resident = personalInfoService.getPersonalInfo(currentUser.getId());
            if (resident != null) {
                // Edit mode
                isEditMode = true;
                titleLabel.setText("Cập nhật thông tin cá nhân");
                saveButton.setText("Cập nhật");
                
                // Fill form
                fullNameField.setText(resident.getFullName());
                idCardField.setText(resident.getIdCard());
                if (resident.getDateOfBirth() != null) {
                    dateOfBirthPicker.setValue(resident.getDateOfBirth());
                }
                if (resident.getGender() != null) {
                    String gender = resident.getGender().equals("male") ? "Nam" : 
                                   resident.getGender().equals("female") ? "Nữ" : resident.getGender();
                    genderComboBox.setValue(gender);
                }
                phoneField.setText(resident.getPhone());
                emailField.setText(resident.getEmail());
                occupationField.setText(resident.getOccupation());
                permanentAddressArea.setText(resident.getPermanentAddress());
                temporaryAddressArea.setText(resident.getTemporaryAddress());
                apartmentCodeField.setText(resident.getApartmentCode());
                householdCodeField.setText(resident.getHouseholdCode());
                relationshipComboBox.setValue(resident.getRelationship());
                
                // Set status
                if (resident.getStatus() != null) {
                    String statusDisplay = resident.getStatusDisplay();
                    statusComboBox.setValue(statusDisplay);
                } else {
                    statusComboBox.setValue("Đang ở");
                }
            } else {
                // Register mode
                isEditMode = false;
                titleLabel.setText("Đăng ký thông tin cá nhân");
                saveButton.setText("Đăng ký");
                
                // Pre-fill with user info
                User user = SessionManager.getInstance().getCurrentUser();
                if (user != null) {
                    fullNameField.setText(user.getFullName());
                    phoneField.setText(user.getPhone());
                    emailField.setText(user.getEmail());
                }
            }
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi tải thông tin: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                ErrorDialog.showError("Lỗi", "Bạn chưa đăng nhập");
                return;
            }
            
            // Validate
            if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
                showMessage("Vui lòng nhập họ và tên", true);
                return;
            }
            
            if (idCardField.getText() == null || idCardField.getText().trim().isEmpty()) {
                showMessage("Vui lòng nhập CMND/CCCD", true);
                return;
            }
            
            // Create request
            PersonalInfoRequest request = new PersonalInfoRequest();
            request.setFullName(fullNameField.getText().trim());
            request.setIdCard(idCardField.getText().trim());
            request.setDateOfBirth(dateOfBirthPicker.getValue());
            
            String gender = genderComboBox.getValue();
            if (gender != null) {
                request.setGender(gender.equals("Nam") ? "male" : gender.equals("Nữ") ? "female" : gender);
            }
            
            request.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : null);
            request.setEmail(emailField.getText() != null ? emailField.getText().trim() : null);
            request.setOccupation(occupationField.getText() != null ? occupationField.getText().trim() : null);
            request.setPermanentAddress(permanentAddressArea.getText() != null ? permanentAddressArea.getText().trim() : null);
            request.setTemporaryAddress(temporaryAddressArea.getText() != null ? temporaryAddressArea.getText().trim() : null);
            request.setApartmentCode(apartmentCodeField.getText() != null ? apartmentCodeField.getText().trim() : null);
            request.setHouseholdCode(householdCodeField.getText() != null ? householdCodeField.getText().trim() : null);
            request.setRelationship(relationshipComboBox.getValue());
            
            // Convert status display to database value
            String statusDisplay = statusComboBox.getValue();
            String statusValue = "active"; // Mặc định
            if (statusDisplay != null) {
                switch (statusDisplay) {
                    case "Đang ở":
                        statusValue = "active";
                        break;
                    case "Tạm trú":
                        statusValue = "temporary_resident";
                        break;
                    case "Tạm vắng":
                        statusValue = "temporary_absent";
                        break;
                    default:
                        statusValue = "active";
                }
            }
            request.setStatus(statusValue);
            
            // Save
            personalInfoService.registerOrUpdatePersonalInfo(currentUser.getId(), request);
            
            showMessage(isEditMode ? "Cập nhật thông tin thành công!" : "Đăng ký thông tin thành công!", false);
            
            // Close after 1 second
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> closeWindow());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (DbException e) {
            ErrorDialog.showDbError("Lỗi khi lưu thông tin: " + e.getMessage());
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
    
    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}

