package vn.bluemoon.ui.fee;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.service.FeeCollectionService;
import vn.bluemoon.util.ErrorDialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for fee collection management view
 */
public class FeeCollectionController {
    @FXML
    private TextField searchApartmentCodeField;
    
    @FXML
    private TextField searchHouseholdCodeField;
    
    @FXML
    private TextField searchOwnerNameField;
    
    @FXML
    private ComboBox<Integer> searchMonthCombo;
    
    @FXML
    private ComboBox<Integer> searchYearCombo;
    
    @FXML
    private ComboBox<String> searchStatusCombo;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button addFeeButton;
    
    @FXML
    private Button markPaidButton;
    
    @FXML
    private TableView<FeeCollection> feeTable;
    
    @FXML
    private TableColumn<FeeCollection, Integer> sttColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> apartmentCodeColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> householdCodeColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> ownerNameColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> monthYearColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> amountColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> statusColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> paymentDateColumn;
    
    @FXML
    private Label totalLabel;
    
    @FXML
    private Label paidLabel;
    
    @FXML
    private Label unpaidLabel;
    
    private FeeCollectionService feeService = new FeeCollectionService();
    private ObservableList<FeeCollection> feeList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Integer defaultYear; // Lưu năm mặc định để phân biệt với năm được chọn
    
    @FXML
    public void initialize() {
        try {
            setupComboBoxes();
            setupTableColumns();
            loadAllFeeCollections();
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi khởi tạo", "Lỗi khi khởi tạo màn hình quản lý thu phí: " + e.getMessage());
        }
    }
    
    private void setupComboBoxes() {
        // Tháng
        searchMonthCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        
        // Năm (từ 2020 đến năm hiện tại + 1)
        int currentYear = LocalDate.now().getYear();
        defaultYear = currentYear; // Lưu năm mặc định
        for (int year = 2020; year <= currentYear + 1; year++) {
            searchYearCombo.getItems().add(year);
        }
        searchYearCombo.setValue(currentYear);
        
        // Trạng thái
        searchStatusCombo.getItems().addAll("Tất cả", "Chưa thu phí", "Đã thu phí");
        searchStatusCombo.setValue("Tất cả");
    }
    
    private void setupTableColumns() {
        sttColumn.setCellValueFactory(param -> {
            int index = feeTable.getItems().indexOf(param.getValue());
            return javafx.beans.binding.Bindings.createObjectBinding(() -> index + 1);
        });
        
        apartmentCodeColumn.setCellValueFactory(new PropertyValueFactory<>("apartmentCode"));
        householdCodeColumn.setCellValueFactory(new PropertyValueFactory<>("householdCode"));
        ownerNameColumn.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        
        monthYearColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getMonthYearDisplay()
            )
        );
        
        amountColumn.setCellValueFactory(param -> {
            BigDecimal amount = param.getValue().getAmount();
            return javafx.beans.binding.Bindings.createStringBinding(
                () -> amount != null ? String.format("%,.0f", amount.doubleValue()) + " đ" : "0 đ"
            );
        });
        
        statusColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getStatusDisplay()
            )
        );
        
        paymentDateColumn.setCellValueFactory(param -> {
            LocalDate date = param.getValue().getPaymentDate();
            return javafx.beans.binding.Bindings.createStringBinding(
                () -> date != null ? date.format(dateFormatter) : ""
            );
        });
    }
    
    @FXML
    private void handleSearch() {
        String apartmentCode = searchApartmentCodeField.getText();
        String householdCode = searchHouseholdCodeField.getText();
        String ownerName = searchOwnerNameField.getText();
        Integer month = searchMonthCombo.getValue();
        Integer year = searchYearCombo.getValue();
        String status = searchStatusCombo.getValue();
        
        // Chỉ coi year là điều kiện tìm kiếm nếu người dùng thực sự chọn (không phải mặc định)
        // Hoặc nếu có filter khác, thì mới áp dụng year
        boolean hasOtherFilters = (apartmentCode != null && !apartmentCode.trim().isEmpty()) ||
                                 (householdCode != null && !householdCode.trim().isEmpty()) ||
                                 (ownerName != null && !ownerName.trim().isEmpty()) ||
                                 month != null;
        
        // Nếu chỉ filter theo status mà không có filter khác, không áp dụng year mặc định
        boolean shouldFilterByYear = hasOtherFilters || (year != null && !year.equals(defaultYear));
        Integer yearToSearch = shouldFilterByYear ? year : null;
        
        // Kiểm tra xem có điều kiện tìm kiếm nào không
        boolean hasSearchCriteria = (apartmentCode != null && !apartmentCode.trim().isEmpty()) ||
                                    (householdCode != null && !householdCode.trim().isEmpty()) ||
                                    (ownerName != null && !ownerName.trim().isEmpty()) ||
                                    month != null ||
                                    shouldFilterByYear ||
                                    (status != null && !status.equals("Tất cả"));
        
        // Convert status display to database value
        String statusValue = null;
        if (status != null && !status.equals("Tất cả")) {
            if (status.equals("Đã thu phí")) {
                statusValue = "paid";
            } else if (status.equals("Chưa thu phí")) {
                statusValue = "unpaid";
            } else {
                statusValue = status; // partial_paid, overpaid, etc.
            }
        }
        
        try {
            List<FeeCollection> fees;
            
            if (!hasSearchCriteria) {
                // Nếu không có điều kiện tìm kiếm, lấy tất cả
                fees = feeService.getAllFeeCollections();
            } else {
                // Có điều kiện tìm kiếm, gọi search
                fees = feeService.searchFeeCollections(
                    (apartmentCode == null || apartmentCode.trim().isEmpty()) ? null : apartmentCode.trim(),
                    (householdCode == null || householdCode.trim().isEmpty()) ? null : householdCode.trim(),
                    (ownerName == null || ownerName.trim().isEmpty()) ? null : ownerName.trim(),
                    month,
                    yearToSearch,
                    statusValue
                );
            }
            
            feeList.clear();
            feeList.addAll(fees);
            feeTable.setItems(feeList);
            updateStatistics();
            
            // Debug: In ra số lượng kết quả tìm được
            System.out.println("Tìm thấy " + fees.size() + " kết quả");
            if (fees.isEmpty() && hasSearchCriteria) {
                ErrorDialog.showInfo("Thông báo", "Không tìm thấy kết quả nào phù hợp với điều kiện tìm kiếm.");
            }
        } catch (DbException e) {
            e.printStackTrace();
            ErrorDialog.showDbError("Lỗi tìm kiếm: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Lỗi khi tìm kiếm: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        searchApartmentCodeField.clear();
        searchHouseholdCodeField.clear();
        searchOwnerNameField.clear();
        searchMonthCombo.setValue(null);
        searchYearCombo.setValue(LocalDate.now().getYear());
        searchStatusCombo.setValue("Tất cả");
        loadAllFeeCollections();
    }
    
    @FXML
    private void handleAddFee() {
        showAddFeeDialog();
    }
    
    private void showAddFeeDialog() {
        Dialog<FeeCollection> dialog = new Dialog<>();
        dialog.setTitle("Thêm thu phí");
        dialog.setHeaderText("Thêm thu phí định kỳ");
        
        // VBox chứa form thu phí định kỳ
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        ComboBox<String> householdCombo = new ComboBox<>();
        CheckBox selectAllCheckbox = new CheckBox("Chọn tất cả các hộ dân");
        TextField amountField = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<Integer> monthCombo = new ComboBox<>();
        ComboBox<Integer> yearCombo = new ComboBox<>();
        
        // Load households - CHỈ LẤY CÁC HỘ CÓ CHỦ HỘ VỚI USER_ID
        List<Resident> allResidents;
        try {
            List<Resident> allResidentsTemp = new vn.bluemoon.repository.ResidentRepository().findAll();
            // Lọc chỉ lấy các hộ có chủ hộ với user_id (đã đăng ký thông tin cá nhân) VÀ relationship = 'Chủ hộ'
            allResidents = new java.util.ArrayList<>();
            for (Resident r : allResidentsTemp) {
                if (r.getUserId() != null && "Chủ hộ".equals(r.getRelationship())) {
                    allResidents.add(r);
                }
            }
            ObservableList<String> householdItems = FXCollections.observableArrayList();
            for (Resident r : allResidents) {
                String display = r.getFullName() + " - " + r.getApartmentCode() + " - " + r.getHouseholdCode();
                householdItems.add(display);
            }
            householdCombo.setItems(householdItems);
        } catch (Exception e) {
            ErrorDialog.showError("Lỗi", "Không thể tải danh sách hộ dân: " + e.getMessage());
            return;
        }
        
        // Setup month/year combos
        monthCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthCombo.setValue(LocalDate.now().getMonthValue());
        int currentYear = LocalDate.now().getYear();
        for (int year = 2020; year <= currentYear + 1; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(currentYear);
        
        // Handle checkbox change
        selectAllCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            householdCombo.setDisable(newVal);
            if (newVal) {
                householdCombo.setValue(null);
            }
        });
        
        content.getChildren().addAll(
            selectAllCheckbox,
            new Label("Chọn hộ dân:"),
            householdCombo,
            new Label("Tháng:"),
            monthCombo,
            new Label("Năm:"),
            yearCombo,
            new Label("Số tiền (VNĐ):"),
            amountField,
            new Label("Ngày thu phí:"),
            datePicker
        );
        dialog.getDialogPane().setContent(content);
        
        ButtonType saveButton = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                try {
                    // Thu phí định kỳ
                    if (amountField.getText().trim().isEmpty()) {
                        ErrorDialog.showError("Lỗi", "Vui lòng nhập số tiền");
                        return null;
                    }
                    
                    BigDecimal amount = new BigDecimal(amountField.getText().trim().replaceAll("[^0-9]", ""));
                    Integer month = monthCombo.getValue();
                    Integer year = yearCombo.getValue();
                    
                    if (selectAllCheckbox.isSelected()) {
                        // Tạo cho tất cả các hộ dân
                        return createFeeForAllHouseholds(allResidents, month, year, amount, "periodic", null);
                    } else {
                        // Tạo cho 1 hộ dân
                        if (householdCombo.getValue() == null) {
                            ErrorDialog.showError("Lỗi", "Vui lòng chọn hộ dân hoặc chọn tất cả");
                            return null;
                        }
                        
                        String selected = householdCombo.getValue();
                        Integer householdId = extractHouseholdId(selected);
                        if (householdId == null) {
                            ErrorDialog.showError("Lỗi", "Không tìm thấy hộ dân");
                            return null;
                        }
                        
                        FeeCollection fee = new FeeCollection();
                        fee.setHouseholdId(householdId);
                        fee.setMonth(month);
                        fee.setYear(year);
                        fee.setAmount(amount);
                        fee.setFeeType("periodic");
                        fee.setStatus("unpaid");
                        fee.setPaidAmount(BigDecimal.ZERO);
                        return fee;
                    }
                } catch (NumberFormatException e) {
                    ErrorDialog.showError("Lỗi", "Số tiền không hợp lệ");
                    return null;
                } catch (Exception e) {
                    ErrorDialog.showError("Lỗi", "Lỗi khi tạo thu phí: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(fee -> {
            try {
                if (fee != null && fee.getHouseholdId() != null) {
                    // Single household - chưa được tạo trong resultConverter
                    System.out.println("=== DEBUG CREATE FEE ===");
                    System.out.println("Creating fee for household_id: " + fee.getHouseholdId());
                    System.out.println("Month: " + fee.getMonth() + ", Year: " + fee.getYear());
                    System.out.println("Amount: " + fee.getAmount() + ", FeeType: " + fee.getFeeType());
                    
                    // Kiểm tra xem household này có resident với user_id không
                    try {
                        String checkSql = "SELECT r.id, r.user_id, r.relationship FROM residents r WHERE r.household_id = ? AND r.relationship = 'Chủ hộ'";
                        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                            stmt.setInt(1, fee.getHouseholdId());
                            java.sql.ResultSet rs = stmt.executeQuery();
                            while (rs.next()) {
                                System.out.println("  Resident ID: " + rs.getInt("id") + 
                                                 ", User ID: " + rs.getObject("user_id") + 
                                                 ", Relationship: " + rs.getString("relationship"));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking resident: " + e.getMessage());
                    }
                    
                    FeeCollection created = feeService.createFeeCollection(fee.getHouseholdId(), fee.getMonth(), fee.getYear(), fee.getAmount());
                    if (created != null) {
                        System.out.println("Fee created successfully - ID: " + created.getId() + ", Status: " + created.getStatus());
                        created.setFeeType(fee.getFeeType());
                        created.setReason(fee.getReason());
                        feeService.updateFeeCollection(created);
                        loadAllFeeCollections();
                        ErrorDialog.showInfo("Thành công", "Đã thêm thu phí thành công");
                    } else {
                        System.out.println("Fee creation returned null!");
                    }
                    System.out.println("=== END DEBUG ===");
                } else if (fee != null) {
                    // Đã xử lý "tất cả" trong createFeeForAllHouseholds, chỉ cần reload
                    loadAllFeeCollections();
                }
            } catch (DbException e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("fee_type")) {
                    ErrorDialog.showError("Lỗi Database", 
                        "Cột 'fee_type' chưa tồn tại trong database.\n\n" +
                        "Vui lòng chạy migration script:\n" +
                        "blue-moon-app/src/main/resources/sql/migration-add-fee-type-reason.sql");
                } else if (errorMsg != null && (errorMsg.contains("duplicate key") || errorMsg.contains("unique constraint"))) {
                    ErrorDialog.showError("Lỗi", 
                        "Đã tồn tại thu phí cho hộ dân này trong tháng/năm này.\n\n" +
                        "Vui lòng chạy migration script để sửa constraint:\n" +
                        "blue-moon-app/src/main/resources/sql/migration-add-fee-type-reason.sql\n\n" +
                        "Hoặc chọn hộ dân và tháng/năm khác.");
                } else {
                    ErrorDialog.showDbError(errorMsg);
                }
            } catch (Exception e) {
                ErrorDialog.showError("Lỗi", "Lỗi không xác định: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private Integer extractHouseholdId(String displayText) {
        try {
            // Format: "Tên - Mã căn hộ - Mã hộ dân"
            String[] parts = displayText.split(" - ");
            if (parts.length >= 3) {
                String householdCode = parts[2];
                String sql = "SELECT id FROM households WHERE household_code = ?";
                try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, householdCode);
                    java.sql.ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Tạo thu phí cho tất cả các hộ dân
     * @return FeeCollection với householdId = null để đánh dấu đã xử lý
     */
    private FeeCollection createFeeForAllHouseholds(List<Resident> residents, Integer month, Integer year, 
                                                     BigDecimal amount, String feeType, String reason) {
        int successCount = 0;
        int failCount = 0;
        String firstError = null;
        
        for (Resident resident : residents) {
            try {
                FeeCollection fee = new FeeCollection();
                fee.setHouseholdId(resident.getHouseholdId());
                fee.setMonth(month);
                fee.setYear(year);
                fee.setAmount(amount);
                fee.setFeeType(feeType);
                fee.setReason(reason);
                fee.setStatus("unpaid");
                fee.setPaidAmount(BigDecimal.ZERO);
                
                FeeCollection created = feeService.createFeeCollection(fee.getHouseholdId(), fee.getMonth(), fee.getYear(), fee.getAmount());
                if (created != null) {
                    created.setFeeType(fee.getFeeType());
                    created.setReason(fee.getReason());
                    feeService.updateFeeCollection(created);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (DbException e) {
                failCount++;
                if (firstError == null) {
                    firstError = e.getMessage();
                }
                System.err.println("Lỗi khi tạo thu phí cho hộ " + resident.getHouseholdCode() + ": " + e.getMessage());
            } catch (Exception e) {
                failCount++;
                if (firstError == null) {
                    firstError = e.getMessage();
                }
                System.err.println("Lỗi khi tạo thu phí cho hộ " + resident.getHouseholdCode() + ": " + e.getMessage());
            }
        }
        
        // Hiển thị thông báo kết quả
        if (successCount > 0) {
            String message = String.format("Đã tạo thu phí cho %d hộ dân", successCount);
            if (failCount > 0) {
                message += String.format(" (%d hộ dân thất bại)", failCount);
                if (firstError != null && firstError.contains("fee_type")) {
                    message += "\n\nLỗi: Cột 'fee_type' chưa tồn tại. Vui lòng chạy migration script.";
                }
            }
            ErrorDialog.showInfo("Thành công", message);
        } else {
            // Tất cả đều thất bại
            String errorMsg = "Không thể tạo thu phí cho bất kỳ hộ dân nào.";
            if (firstError != null) {
                if (firstError.contains("fee_type")) {
                    errorMsg = "Cột 'fee_type' chưa tồn tại trong database.\n\n" +
                              "Vui lòng chạy migration script:\n" +
                              "blue-moon-app/src/main/resources/sql/migration-add-fee-type-reason.sql";
                } else {
                    errorMsg += "\n\nLỗi đầu tiên: " + firstError;
                }
            }
            ErrorDialog.showError("Lỗi", errorMsg);
        }
        
        // Return dummy object với householdId = null để đánh dấu đã xử lý
        FeeCollection dummy = new FeeCollection();
        dummy.setHouseholdId(null);
        return dummy;
    }
    
    @FXML
    private void handleMarkPaid() {
        FeeCollection selected = feeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn một bản ghi thu phí");
            return;
        }
        
        if ("paid".equals(selected.getStatus())) {
            ErrorDialog.showInfo("Thông tin", "Bản ghi này đã được đánh dấu là đã thu phí");
            return;
        }
        
        // Lưu ID của bản ghi được chọn để tìm lại sau
        Integer selectedId = selected.getId();
        
        // Hiển thị dialog để nhập thông tin thanh toán
        Dialog<PaymentInfo> dialog = createPaymentDialog();
        dialog.showAndWait().ifPresent(paymentInfo -> {
            try {
                feeService.markAsPaid(selectedId, paymentInfo.date, paymentInfo.method);
                
                // Lưu status filter hiện tại
                String currentStatus = searchStatusCombo.getValue();
                
                // Nếu đang filter theo "Chưa thu phí", reset về "Tất cả" để bản ghi vẫn hiển thị
                if (currentStatus != null && currentStatus.equals("Chưa thu phí")) {
                    searchStatusCombo.setValue("Tất cả");
                }
                
                // Reload lại danh sách với filter mới (sau khi đã reset status nếu cần)
                handleSearch();
                
                // Tìm và select lại bản ghi vừa cập nhật
                boolean found = false;
                for (FeeCollection fee : feeList) {
                    if (fee.getId().equals(selectedId)) {
                        feeTable.getSelectionModel().select(fee);
                        feeTable.scrollTo(fee);
                        found = true;
                        break;
                    }
                }
                
                // Nếu không tìm thấy bản ghi (do filter), reload toàn bộ và tìm lại
                if (!found) {
                    // Clear tất cả filter để hiển thị toàn bộ
                    searchApartmentCodeField.clear();
                    searchHouseholdCodeField.clear();
                    searchOwnerNameField.clear();
                    searchMonthCombo.setValue(null);
                    searchYearCombo.setValue(LocalDate.now().getYear());
                    searchStatusCombo.setValue("Tất cả");
                    
                    loadAllFeeCollections();
                    
                    // Tìm lại bản ghi
                    for (FeeCollection fee : feeList) {
                        if (fee.getId().equals(selectedId)) {
                            feeTable.getSelectionModel().select(fee);
                            feeTable.scrollTo(fee);
                            break;
                        }
                    }
                }
                
                ErrorDialog.showInfo("Thành công", "Đã đánh dấu đã thu phí thành công");
            } catch (DbException e) {
                ErrorDialog.showDbError(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                ErrorDialog.showError("Lỗi", "Lỗi khi đánh dấu đã thu phí: " + e.getMessage());
            }
        });
    }
    
    private Dialog<PaymentInfo> createPaymentDialog() {
        Dialog<PaymentInfo> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận thu phí");
        dialog.setHeaderText("Nhập thông tin thanh toán");
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("Chuyển khoản", "Thẻ tín dụng");
        methodCombo.setValue("Chuyển khoản");
        
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
            new Label("Ngày thanh toán:"),
            datePicker,
            new Label("Phương thức thanh toán:"),
            methodCombo
        );
        dialog.getDialogPane().setContent(vbox);
        
        ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButton) {
                String method = methodCombo.getValue();
                String methodValue = method.equals("Chuyển khoản") ? "bank_transfer" : "credit_card";
                return new PaymentInfo(datePicker.getValue(), methodValue);
            }
            return null;
        });
        
        return dialog;
    }
    
    private void loadAllFeeCollections() {
        try {
            List<FeeCollection> fees = feeService.getAllFeeCollections();
            feeList.clear();
            feeList.addAll(fees);
            feeTable.setItems(feeList);
            updateStatistics();
        } catch (DbException e) {
            // Nếu bảng chưa tồn tại, hiển thị thông báo và để danh sách trống
            if (e.getMessage().contains("does not exist") || e.getMessage().contains("relation") || e.getMessage().contains("table")) {
                ErrorDialog.showInfo("Thông báo", "Bảng fee_collections chưa được tạo. Vui lòng chạy file schema-fee-postgresql.sql trong database.");
            } else {
                ErrorDialog.showDbError(e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
    }
    
    private void updateStatistics() {
        int total = feeList.size();
        long paid = feeList.stream().filter(f -> "paid".equals(f.getStatus())).count();
        long unpaid = total - paid;
        
        totalLabel.setText("Tổng số: " + total);
        paidLabel.setText("Đã thu: " + paid);
        unpaidLabel.setText("Chưa thu: " + unpaid);
    }
    
    private static class PaymentInfo {
        LocalDate date;
        String method;
        
        PaymentInfo(LocalDate date, String method) {
            this.date = date;
            this.method = method;
        }
    }
}

