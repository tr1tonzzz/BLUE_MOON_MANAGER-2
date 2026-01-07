package vn.bluemoon.ui.payment;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.security.SessionManager;
import vn.bluemoon.service.PaymentService;
import vn.bluemoon.util.ErrorDialog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for payment view
 */
public class PaymentController {
    @FXML
    private Label totalRemainingLabel;
    
    @FXML
    private TableView<FeeCollection> feeTable;
    
    @FXML
    private TableColumn<FeeCollection, String> monthYearColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> amountColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> paidAmountColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> remainingColumn;
    
    @FXML
    private TableColumn<FeeCollection, String> statusColumn;
    
    @FXML
    private ComboBox<FeeCollection> feeComboBox;
    
    @FXML
    private TextField amountField;
    
    @FXML
    private ComboBox<String> paymentMethodComboBox;
    
    @FXML
    private Button payButton;
    
    @FXML
    private Label selectedFeeInfoLabel;
    
    private PaymentService paymentService = new PaymentService();
    private ObservableList<FeeCollection> feeList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupTableColumns();
        setupPaymentMethodComboBox();
        loadUnpaidFees();
        setupFeeComboBox();
    }
    
    private void setupTableColumns() {
        monthYearColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getMonthYearDisplay()
            )
        );
        
        amountColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> formatCurrency(param.getValue().getAmount())
            )
        );
        
        paidAmountColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> formatCurrency(param.getValue().getPaidAmount())
            )
        );
        
        remainingColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    BigDecimal remaining = param.getValue().getRemainingAmount();
                    String formatted = formatCurrency(remaining.abs());
                    if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                        return "-" + formatted + " (dư)";
                    }
                    return formatted;
                }
            )
        );
        
        statusColumn.setCellValueFactory(param -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> param.getValue().getStatusDisplay()
            )
        );
    }
    
    private void setupPaymentMethodComboBox() {
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
            "Chuyển khoản", "Thẻ tín dụng"
        ));
        paymentMethodComboBox.getSelectionModel().selectFirst();
    }
    
    private void setupFeeComboBox() {
        feeComboBox.setItems(feeList);
        feeComboBox.setCellFactory(param -> new ListCell<FeeCollection>() {
            @Override
            protected void updateItem(FeeCollection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    BigDecimal remaining = item.getRemainingAmount();
                    String remainingText = remaining.compareTo(BigDecimal.ZERO) < 0 
                        ? formatCurrency(remaining.abs()) + " (dư)" 
                        : formatCurrency(remaining);
                    setText(String.format("%s - Còn lại: %s", 
                        item.getMonthYearDisplay(), remainingText));
                }
            }
        });
        
        feeComboBox.setButtonCell(new ListCell<FeeCollection>() {
            @Override
            protected void updateItem(FeeCollection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    BigDecimal remaining = item.getRemainingAmount();
                    String remainingText = remaining.compareTo(BigDecimal.ZERO) < 0 
                        ? formatCurrency(remaining.abs()) + " (dư)" 
                        : formatCurrency(remaining);
                    setText(String.format("%s - Còn lại: %s", 
                        item.getMonthYearDisplay(), remainingText));
                }
            }
        });
        
        feeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateSelectedFeeInfo(newVal);
            }
        });
    }
    
    private void updateSelectedFeeInfo(FeeCollection fee) {
        BigDecimal remaining = fee.getRemainingAmount();
        String info = String.format("Tháng: %s | Tổng: %s | Đã nộp: %s | Còn lại: %s",
            fee.getMonthYearDisplay(),
            formatCurrency(fee.getAmount()),
            formatCurrency(fee.getPaidAmount()),
            remaining.compareTo(BigDecimal.ZERO) < 0 
                ? formatCurrency(remaining.abs()) + " (dư)" 
                : formatCurrency(remaining)
        );
        selectedFeeInfoLabel.setText(info);
        amountField.setText(remaining.compareTo(BigDecimal.ZERO) > 0 
            ? remaining.toString() 
            : "0");
    }
    
    private void loadUnpaidFees() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                ErrorDialog.showError("Lỗi", "Vui lòng đăng nhập lại");
                return;
            }
            
            // Debug: Kiểm tra xem user có resident record không
            vn.bluemoon.model.entity.Resident resident = new vn.bluemoon.repository.ResidentRepository().findByUserId(currentUser.getId());
            if (resident == null) {
                ErrorDialog.showInfo("Thông báo", 
                    "Bạn chưa đăng ký thông tin cá nhân.\n\n" +
                    "Vui lòng vào menu 'Cá nhân' → 'Thông tin cá nhân' để đăng ký thông tin trước.");
                feeList.clear();
                feeTable.setItems(feeList);
                totalRemainingLabel.setText("Tổng số tiền cần đóng: 0 đ");
                totalRemainingLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
            
            // Debug: Kiểm tra fee collections của household
            System.out.println("=== DEBUG PAYMENT ===");
            System.out.println("User ID: " + currentUser.getId());
            System.out.println("Resident ID: " + resident.getId() + ", Household ID: " + resident.getHouseholdId());
            System.out.println("Resident Relationship: " + resident.getRelationship());
            
            // Kiểm tra trực tiếp trong database
            try {
                String directSql = "SELECT fc.*, r.user_id, r.relationship " +
                                  "FROM fee_collections fc " +
                                  "JOIN households h ON fc.household_id = h.id " +
                                  "LEFT JOIN residents r ON r.household_id = h.id AND r.relationship = 'Chủ hộ' " +
                                  "WHERE fc.household_id = ?";
                try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(directSql)) {
                    stmt.setInt(1, resident.getHouseholdId());
                    java.sql.ResultSet rs = stmt.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println("  Direct DB Query - Fee ID: " + rs.getInt("id") + 
                                         ", Month/Year: " + rs.getObject("month") + "/" + rs.getObject("year") +
                                         ", Status: " + rs.getString("status") +
                                         ", Amount: " + rs.getBigDecimal("amount") +
                                         ", Resident user_id: " + rs.getObject("user_id") +
                                         ", Relationship: " + rs.getString("relationship"));
                    }
                    System.out.println("Total fee collections in DB for household " + resident.getHouseholdId() + ": " + count);
                }
            } catch (Exception e) {
                System.err.println("Error in direct DB query: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Kiểm tra xem có fee collections nào cho household này không (qua repository)
            List<FeeCollection> allFeesForHousehold = new java.util.ArrayList<>();
            try {
                allFeesForHousehold = new vn.bluemoon.repository.FeeCollectionRepository().findByHouseholdId(resident.getHouseholdId());
                System.out.println("Total fee collections via repository for household " + resident.getHouseholdId() + ": " + allFeesForHousehold.size());
                for (FeeCollection fc : allFeesForHousehold) {
                    System.out.println("  - Fee ID: " + fc.getId() + ", Month/Year: " + fc.getMonth() + "/" + fc.getYear() + 
                                     ", Status: " + fc.getStatus() + ", Amount: " + fc.getAmount());
                }
            } catch (Exception e) {
                System.err.println("Error checking household fees: " + e.getMessage());
            }
            
            feeList.clear();
            List<FeeCollection> unpaidFees = paymentService.getUnpaidFeesForUser(currentUser.getId());
            feeList.addAll(unpaidFees);
            feeTable.setItems(feeList);
            
            // Debug log
            System.out.println("Found " + unpaidFees.size() + " unpaid fees for user");
            
            // Kiểm tra tất cả fees (bao gồm cả paid) để debug
            try {
                List<FeeCollection> allFeesForUser = new vn.bluemoon.repository.FeeCollectionRepository().findByUserId(currentUser.getId());
                System.out.println("All fees for user (including paid): " + allFeesForUser.size());
                int unpaidCount = 0, paidCount = 0, partialCount = 0, overpaidCount = 0;
                for (FeeCollection fc : allFeesForUser) {
                    String status = fc.getStatus();
                    System.out.println("  - Fee ID: " + fc.getId() + ", Status: " + status + 
                                     ", Month/Year: " + fc.getMonth() + "/" + fc.getYear() +
                                     ", Amount: " + fc.getAmount() + ", Paid: " + fc.getPaidAmount());
                    if ("unpaid".equals(status)) unpaidCount++;
                    else if ("paid".equals(status)) paidCount++;
                    else if ("partial_paid".equals(status)) partialCount++;
                    else if ("overpaid".equals(status)) overpaidCount++;
                }
                System.out.println("Status breakdown - Unpaid: " + unpaidCount + ", Paid: " + paidCount + 
                                 ", Partial: " + partialCount + ", Overpaid: " + overpaidCount);
            } catch (Exception e) {
                System.err.println("Error getting all fees: " + e.getMessage());
                e.printStackTrace();
            }
            
            if (unpaidFees.isEmpty() && !allFeesForHousehold.isEmpty()) {
                System.out.println("WARNING: Có fee collections cho household nhưng không có unpaid fees!");
                System.out.println("Tất cả fees có thể đã được đánh dấu là 'paid'");
                System.out.println("Nếu bạn vừa tạo fee mới, hãy kiểm tra xem fee đó có được tạo với đúng household_id không");
            }
            System.out.println("=== END DEBUG ===");
            
            // Cập nhật tổng số tiền còn lại
            BigDecimal totalRemaining = paymentService.getTotalRemainingAmount(currentUser.getId());
            if (totalRemaining.compareTo(BigDecimal.ZERO) < 0) {
                totalRemainingLabel.setText(String.format("Tổng số dư: %s", 
                    formatCurrency(totalRemaining.abs())));
                totalRemainingLabel.setStyle("-fx-text-fill: green;");
            } else {
                totalRemainingLabel.setText(String.format("Tổng số tiền cần đóng: %s", 
                    formatCurrency(totalRemaining)));
                totalRemainingLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError("Lỗi", "Lỗi khi tải danh sách thu phí: " + e.getMessage());
        }
    }
    
    @FXML
    private void handlePay() {
        FeeCollection selectedFee = feeComboBox.getSelectionModel().getSelectedItem();
        if (selectedFee == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn khoản phí cần thanh toán");
            return;
        }
        
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            ErrorDialog.showError("Lỗi", "Vui lòng nhập số tiền");
            return;
        }
        
        BigDecimal paymentAmount;
        try {
            paymentAmount = new BigDecimal(amountText);
            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                ErrorDialog.showError("Lỗi", "Số tiền phải lớn hơn 0");
                return;
            }
        } catch (NumberFormatException e) {
            ErrorDialog.showError("Lỗi", "Số tiền không hợp lệ");
            return;
        }
        
        String paymentMethod = paymentMethodComboBox.getSelectionModel().getSelectedItem();
        if (paymentMethod == null) {
            ErrorDialog.showError("Lỗi", "Vui lòng chọn phương thức thanh toán");
            return;
        }
        
        try {
            FeeCollection updatedFee = paymentService.processPayment(
                selectedFee.getId(), 
                paymentAmount, 
                paymentMethod
            );
            
            // Hiển thị thông báo
            BigDecimal remaining = updatedFee.getRemainingAmount();
            String message;
            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                message = "Thanh toán thành công! Đã thanh toán đủ số tiền.";
            } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                message = String.format("Thanh toán thành công! Còn thiếu: %s", 
                    formatCurrency(remaining));
            } else {
                message = String.format("Thanh toán thành công! Bạn đã nộp dư: %s", 
                    formatCurrency(remaining.abs()));
            }
            
            ErrorDialog.showInfo("Thông báo", message);
            
            // Reload data
            loadUnpaidFees();
            feeComboBox.getSelectionModel().select(updatedFee);
            updateSelectedFeeInfo(updatedFee);
            
        } catch (DbException e) {
            ErrorDialog.showDbError(e.getMessage());
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue());
    }
}

