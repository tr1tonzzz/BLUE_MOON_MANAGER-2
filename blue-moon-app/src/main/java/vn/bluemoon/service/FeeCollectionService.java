package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.repository.FeeCollectionRepository;
import vn.bluemoon.validation.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for Fee Collection management
 */
public class FeeCollectionService {
    private final FeeCollectionRepository feeRepository = new FeeCollectionRepository();
    
    /**
     * Get all fee collections
     */
    public List<FeeCollection> getAllFeeCollections() throws DbException {
        return feeRepository.findAll();
    }
    
    /**
     * Search fee collections
     */
    public List<FeeCollection> searchFeeCollections(String apartmentCode, String householdCode, 
                                                    String ownerName, Integer month, Integer year, 
                                                    String status) throws DbException {
        return feeRepository.search(apartmentCode, householdCode, ownerName, month, year, status);
    }
    
    /**
     * Get fee collections by household
     */
    public List<FeeCollection> getFeeCollectionsByHousehold(Integer householdId) throws DbException {
        return feeRepository.findByHouseholdId(householdId);
    }
    
    /**
     * Get fee collections by month and year
     */
    public List<FeeCollection> getFeeCollectionsByMonthYear(Integer month, Integer year) throws DbException {
        return feeRepository.findByMonthYear(month, year);
    }
    
    /**
     * Check if fee collection already exists for a household in a specific month/year
     * Nếu có feeTypeId và database có cột fee_type_id, chỉ kiểm tra duplicate cho cùng fee_type_id
     * Nếu không có feeTypeId hoặc database chưa có cột fee_type_id, không kiểm tra duplicate (cho phép thêm nhiều phí cùng tháng/năm)
     */
    public boolean feeCollectionExists(Integer householdId, Integer month, Integer year, Integer feeTypeId) throws DbException {
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection()) {
            // Kiểm tra xem có cột fee_type_id không
            boolean hasFeeTypeIdColumn = checkColumnExists(conn, "fee_collections", "fee_type_id");
            
            // Chỉ kiểm tra duplicate nếu có cột fee_type_id VÀ feeTypeId không null
            // Nếu không có cột hoặc feeTypeId null, cho phép thêm nhiều phí cùng tháng/năm
            if (!hasFeeTypeIdColumn || feeTypeId == null) {
                System.out.println("DEBUG: feeCollectionExists - hasFeeTypeIdColumn=" + hasFeeTypeIdColumn + ", feeTypeId=" + feeTypeId + " -> Không kiểm tra duplicate (cho phép thêm nhiều phí)");
                return false; // Không kiểm tra duplicate, cho phép thêm
            }
            
            // Kiểm tra duplicate cho cùng fee_type_id
            String sql = "SELECT COUNT(*) FROM fee_collections " +
                        "WHERE household_id = ? AND month = ? AND year = ? AND fee_type_id = ?";
            
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, householdId);
                stmt.setInt(2, month);
                stmt.setInt(3, year);
                stmt.setInt(4, feeTypeId);
                java.sql.ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("DEBUG: feeCollectionExists - householdId=" + householdId + ", month=" + month + ", year=" + year + ", feeTypeId=" + feeTypeId + " -> count=" + count);
                    return count > 0;
                }
            }
        } catch (java.sql.SQLException e) {
            throw new DbException("Error checking existing fee collection: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Backward compatibility: Check if fee collection exists (without feeTypeId)
     */
    public boolean feeCollectionExists(Integer householdId, Integer month, Integer year) throws DbException {
        return feeCollectionExists(householdId, month, year, null);
    }
    
    /**
     * Helper method to check if a column exists in a table
     */
    private boolean checkColumnExists(java.sql.Connection conn, String tableName, String columnName) {
        try {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            java.sql.ResultSet rs = meta.getColumns(null, null, tableName, columnName);
            return rs.next();
        } catch (java.sql.SQLException e) {
            return false;
        }
    }
    
    /**
     * Create fee collection for a household
     * Kiểm tra duplicate trước khi tạo để tránh lỗi unique constraint
     * Nếu có feeTypeId và database có cột fee_type_id, chỉ kiểm tra duplicate cho cùng fee_type_id
     * Nếu không có feeTypeId hoặc database chưa có cột fee_type_id, cho phép thêm nhiều phí cùng tháng/năm
     */
    public FeeCollection createFeeCollection(Integer householdId, Integer month, Integer year, 
                                             BigDecimal amount, Integer feeTypeId, LocalDate paymentDeadline) throws DbException, ValidationException {
        // Kiểm tra xem đã có fee collection cho hộ này trong tháng/năm này với cùng fee_type_id chưa
        // Chỉ kiểm tra nếu có feeTypeId và database có cột fee_type_id
        if (feeCollectionExists(householdId, month, year, feeTypeId)) {
            throw new ValidationException("Đã tồn tại thu phí cho hộ dân này trong tháng/năm này với cùng loại phí dịch vụ. Vui lòng chọn tháng/năm khác hoặc loại phí khác.");
        }
        
        FeeCollection fee = new FeeCollection();
        fee.setHouseholdId(householdId);
        fee.setMonth(month);
        fee.setYear(year);
        fee.setAmount(amount != null ? amount : BigDecimal.ZERO);
        fee.setStatus("unpaid");
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setFeeType("periodic");
        fee.setFeeTypeId(feeTypeId); // Set fee_type_id để phân biệt các loại phí dịch vụ
        fee.setPaymentDeadline(paymentDeadline); // Set hạn thu phí
        
        return feeRepository.create(fee);
    }
    
    /**
     * Backward compatibility: Create fee collection without paymentDeadline
     */
    public FeeCollection createFeeCollection(Integer householdId, Integer month, Integer year, 
                                             BigDecimal amount, Integer feeTypeId) throws DbException, ValidationException {
        return createFeeCollection(householdId, month, year, amount, feeTypeId, null);
    }
    
    /**
     * Backward compatibility: Create fee collection without feeTypeId and paymentDeadline
     */
    public FeeCollection createFeeCollection(Integer householdId, Integer month, Integer year, 
                                             BigDecimal amount) throws DbException, ValidationException {
        return createFeeCollection(householdId, month, year, amount, null, null);
    }
    
    /**
     * Mark fee collection as paid
     */
    public void markAsPaid(Integer feeId, LocalDate paymentDate, String paymentMethod) throws DbException {
        feeRepository.markAsPaid(feeId, paymentDate, paymentMethod);
    }
    
    /**
     * Update fee collection
     */
    public void updateFeeCollection(FeeCollection fee) throws DbException {
        feeRepository.update(fee);
    }
    
    /**
     * Delete fee collection by ID
     */
    public void deleteFeeCollection(Integer id) throws DbException {
        feeRepository.delete(id);
    }
}


