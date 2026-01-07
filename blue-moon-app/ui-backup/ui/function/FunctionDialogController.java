package vn.bluemoon.ui.function;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.FunctionUpsertRequest;
import vn.bluemoon.model.entity.Function;
import vn.bluemoon.model.entity.FunctionGroup;
import vn.bluemoon.service.FunctionService;
import vn.bluemoon.util.ErrorDialog;
import vn.bluemoon.validation.ValidationException;

import java.util.List;

/**
 * Controller for function dialog
 */
public class FunctionDialogController {
    @FXML
    private TextField nameField;
    
    @FXML
    private ComboBox<FunctionGroup> functionGroupComboBox;
    
    @FXML
    private TextField boundaryClassField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Label errorLabel;
    
    private FunctionService functionService = new FunctionService();
    private Function function;
    private FunctionManagementController parentController;
    
    public void setFunction(Function function) {
        this.function = function;
        if (function != null) {
            nameField.setText(function.getName());
            boundaryClassField.setText(function.getBoundaryClass());
            descriptionField.setText(function.getDescription());
        }
    }
    
    public void setParentController(FunctionManagementController parentController) {
        this.parentController = parentController;
    }
    
    @FXML
    public void initialize() {
        try {
            List<FunctionGroup> groups = functionService.getAllFunctionGroups();
            functionGroupComboBox.setItems(FXCollections.observableArrayList(groups));
            functionGroupComboBox.setCellFactory(param -> new ListCell<FunctionGroup>() {
                @Override
                protected void updateItem(FunctionGroup item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            functionGroupComboBox.setButtonCell(new ListCell<FunctionGroup>() {
                @Override
                protected void updateItem(FunctionGroup item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        }
    }
    
    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);
        
        try {
            FunctionUpsertRequest request = new FunctionUpsertRequest();
            if (function != null) {
                request.setId(function.getId());
            }
            request.setName(nameField.getText().trim());
            request.setBoundaryClass(boundaryClassField.getText().trim());
            request.setDescription(descriptionField.getText().trim());
            
            FunctionGroup selectedGroup = functionGroupComboBox.getSelectionModel().getSelectedItem();
            if (selectedGroup == null) {
                showError("Vui lòng chọn nhóm chức năng");
                return;
            }
            request.setFunctionGroupId(selectedGroup.getId());
            
            if (function == null) {
                functionService.createFunction(request);
                ErrorDialog.showInfo("Thành công", "Đã thêm chức năng thành công");
            } else {
                functionService.updateFunction(request);
                ErrorDialog.showInfo("Thành công", "Đã cập nhật chức năng thành công");
            }
            
            if (parentController != null) {
                parentController.refreshTable();
            }
            
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












