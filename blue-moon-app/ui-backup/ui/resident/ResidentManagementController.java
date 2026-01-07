package vn.bluemoon.ui.resident;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.service.ResidentService;
import vn.bluemoon.util.ErrorDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for resident management view
 */
public class ResidentManagementController {
    @FXML
    private TextField searchNameField;
    
    @FXML
    private TextField searchApartmentCodeField;
    
    @FXML
    private TextField searchHouseholdCodeField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private TableView<Resident> residentTable;
    
    @FXML
    private TableColumn<Resident, Integer> sttColumn;
    
    @FXML
    private TableColumn<Resident, String> apartmentCodeColumn;
    
    @FXML
    private TableColumn<Resident, String> householdCodeColumn;
    
    @FXML
    private TableColumn<Resident, String> fullNameColumn;
    
    @FXML
    private TableColumn<Resident, String> idCardColumn;
    
    @FXML
    private TableColumn<Resident, String> dateOfBirthColumn;
    
    @FXML
    private TableColumn<Resident, String> genderColumn;
    
    @FXML
    private TableColumn<Resident, String> relationshipColumn;
    
    @FXML
    private TableColumn<Resident, String> phoneColumn;
    
    @FXML
    private TableColumn<Resident, String> occupationColumn;
    
    @FXML
    private TableColumn<Resident, String> statusColumn;
    
    @FXML
    private Label totalLabel;
    
    private ResidentService residentService = new ResidentService();
    private ObservableList<Resident> residentList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadAllResidents();
    }
    
    private void setupTableColumns() {
        sttColumn.setCellValueFactory(param -> {
            int index = residentTable.getItems().indexOf(param.getValue());
            return javafx.beans.binding.Bindings.createObjectBinding(() -> index + 1);
        });
        
        apartmentCodeColumn.setCellValueFactory(new PropertyValueFactory<>("apartmentCode"));
        householdCodeColumn.setCellValueFactory(new PropertyValueFactory<>("householdCode"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        idCardColumn.setCellValueFactory(new PropertyValueFactory<>("idCard"));
        
        dateOfBirthColumn.setCellValueFactory(param -> {
            LocalDate dob = param.getValue().getDateOfBirth();
            return javafx.beans.binding.Bindings.createStringBinding(
                () -> dob != null ? dob.format(dateFormatter) : ""
            );
        });
        
        genderColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getGenderDisplay()
            )
        );
        
        relationshipColumn.setCellValueFactory(new PropertyValueFactory<>("relationship"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        occupationColumn.setCellValueFactory(new PropertyValueFactory<>("occupation"));
        
        statusColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getStatusDisplay()
            )
        );
    }
    
    @FXML
    private void handleSearch() {
        String name = searchNameField.getText().trim();
        String apartmentCode = searchApartmentCodeField.getText().trim();
        String householdCode = searchHouseholdCodeField.getText().trim();
        
        try {
            List<Resident> residents = residentService.searchResidents(name, apartmentCode, householdCode);
            residentList.clear();
            residentList.addAll(residents);
            updateTotalLabel();
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        searchNameField.clear();
        searchApartmentCodeField.clear();
        searchHouseholdCodeField.clear();
        loadAllResidents();
    }
    
    private void loadAllResidents() {
        try {
            List<Resident> residents = residentService.getAllResidents();
            residentList.clear();
            residentList.addAll(residents);
            residentTable.setItems(residentList);
            updateTotalLabel();
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        }
    }
    
    private void updateTotalLabel() {
        totalLabel.setText("Tổng số: " + residentList.size() + " nhân khẩu");
    }
    
    @FXML
    private void handleDelete() {
        Resident selected = residentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn một nhân khẩu để xóa");
            return;
        }
        
        // Xác nhận xóa
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText("Xóa nhân khẩu");
        confirmDialog.setContentText(
            "Bạn có chắc chắn muốn xóa nhân khẩu này không?\n\n" +
            "Tên: " + selected.getFullName() + "\n" +
            "Mã hộ dân: " + selected.getHouseholdCode() + "\n\n" +
            "LƯU Ý: Nếu đây là chủ hộ, tất cả thu phí liên quan sẽ bị xóa!"
        );
        
        confirmDialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    residentService.deleteResident(selected.getId());
                    ErrorDialog.showInfo("Thành công", "Đã xóa nhân khẩu thành công");
                    loadAllResidents();
                } catch (DbException e) {
                    ErrorDialog.showDbError(e.getMessage());
                }
            }
        });
    }
}


