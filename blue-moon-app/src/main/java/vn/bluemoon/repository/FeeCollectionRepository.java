package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.util.JdbcUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for FeeCollection entity
 */
public class FeeCollectionRepository {
    
    /**
     * Find all fee collections with household and apartment info
     * CHỈ LẤY CÁC HỘ CÓ CHỦ HỘ (relationship = 'Chủ hộ')
     * Đảm bảo mỗi household chỉ có 1 chủ hộ được hiển thị
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     */
    public List<FeeCollection> findAll() throws DbException {
        List<FeeCollection> fees = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        String sql;
        
        if (isPostgreSQL) {
            sql = "SELECT DISTINCT ON (fc.id) fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY fc.id, fc.year DESC, fc.month DESC, fc.created_at DESC";
        } else {
            // MySQL - use subquery to get distinct
            sql = "SELECT fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY fc.year DESC, fc.month DESC, fc.created_at DESC";
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                fees.add(mapResultSetToFeeCollection(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all fee collections: " + e.getMessage(), e);
        }
        return fees;
    }
    
    /**
     * Find fee collections by user ID (through resident)
     * CHỈ LẤY CÁC FEE_COLLECTIONS CỦA CHỦ HỘ
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     * Tìm trực tiếp qua household_id của resident có user_id
     */
    public List<FeeCollection> findByUserId(Integer userId) throws DbException {
        List<FeeCollection> fees = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        String sql;
        
        if (isPostgreSQL) {
            sql = "SELECT DISTINCT ON (fc.id) fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.household_id IN (SELECT household_id FROM residents WHERE user_id = ? AND relationship = 'Chủ hộ') " +
                 "ORDER BY fc.id, fc.year DESC, fc.month DESC";
        } else {
            sql = "SELECT fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.household_id IN (SELECT household_id FROM residents WHERE user_id = ? AND relationship = 'Chủ hộ') " +
                 "ORDER BY fc.year DESC, fc.month DESC";
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fees.add(mapResultSetToFeeCollection(rs));
            }
            
            // Debug log
            System.out.println("Query fee collections for user ID: " + userId);
            System.out.println("Found " + fees.size() + " fee collections");
            if (fees.size() > 0) {
                System.out.println("First fee: household_id=" + fees.get(0).getHouseholdId() + 
                                 ", month=" + fees.get(0).getMonth() + 
                                 ", year=" + fees.get(0).getYear() + 
                                 ", status=" + fees.get(0).getStatus());
            } else {
                System.out.println("No fee collections found. Checking if user has resident record...");
                // Kiểm tra xem user có resident record không
                String checkResidentSql = "SELECT household_id FROM residents WHERE user_id = ? AND relationship = 'Chủ hộ'";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkResidentSql)) {
                    checkStmt.setInt(1, userId);
                    ResultSet checkRs = checkStmt.executeQuery();
                    if (checkRs.next()) {
                        int householdId = checkRs.getInt("household_id");
                        System.out.println("User has resident record with household_id: " + householdId);
                        // Kiểm tra xem có fee collections nào cho household này không
                        String checkFeeSql = "SELECT COUNT(*) FROM fee_collections WHERE household_id = ?";
                        try (PreparedStatement feeStmt = conn.prepareStatement(checkFeeSql)) {
                            feeStmt.setInt(1, householdId);
                            ResultSet feeRs = feeStmt.executeQuery();
                            if (feeRs.next()) {
                                int feeCount = feeRs.getInt(1);
                                System.out.println("Found " + feeCount + " fee collections for household " + householdId);
                            }
                        }
                    } else {
                        System.out.println("User does NOT have resident record with relationship = 'Chủ hộ'");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in findByUserId query: " + e.getMessage());
            e.printStackTrace();
            throw new DbException("Error finding fee collections by user: " + e.getMessage(), e);
        }
        return fees;
    }
    
    /**
     * Find fee collection by ID
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     */
    public FeeCollection findById(Integer id) throws DbException {
        String sql = "SELECT fc.*, " +
                     "a.apartment_code, " +
                     "h.household_code, " +
                     "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                     "FROM fee_collections fc " +
                     "JOIN households h ON fc.household_id = h.id " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE fc.id = ? " +
                     "AND a.apartment_code NOT LIKE 'DEFAULT-%'";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFeeCollection(rs);
            }
        } catch (SQLException e) {
            throw new DbException("Error finding fee collection by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Find fee collections by household ID
     * CHỈ LẤY CÁC HỘ CÓ CHỦ HỘ (relationship = 'Chủ hộ')
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     */
    public List<FeeCollection> findByHouseholdId(Integer householdId) throws DbException {
        List<FeeCollection> fees = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        String sql;
        
        if (isPostgreSQL) {
            sql = "SELECT DISTINCT ON (fc.id) fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.household_id = ? " +
                 "AND EXISTS (SELECT 1 FROM residents r2 WHERE r2.household_id = h.id AND r2.relationship = 'Chủ hộ') " +
                 "ORDER BY fc.id, fc.year DESC, fc.month DESC";
        } else {
            sql = "SELECT fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.household_id = ? " +
                 "AND a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY fc.year DESC, fc.month DESC";
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fees.add(mapResultSetToFeeCollection(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding fee collections by household: " + e.getMessage(), e);
        }
        return fees;
    }
    
    /**
     * Find fee collection by month and year
     * CHỈ LẤY CÁC HỘ CÓ CHỦ HỘ (relationship = 'Chủ hộ')
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     */
    public List<FeeCollection> findByMonthYear(Integer month, Integer year) throws DbException {
        List<FeeCollection> fees = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        String sql;
        
        if (isPostgreSQL) {
            sql = "SELECT DISTINCT ON (fc.id) fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.month = ? AND fc.year = ? " +
                 "AND a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY fc.id, fc.created_at DESC";
        } else {
            sql = "SELECT fc.*, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                 "FROM fee_collections fc " +
                 "JOIN households h ON fc.household_id = h.id " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "WHERE fc.month = ? AND fc.year = ? " +
                 "AND a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY fc.created_at DESC";
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month);
            stmt.setInt(2, year);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fees.add(mapResultSetToFeeCollection(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding fee collections by month/year: " + e.getMessage(), e);
        }
        return fees;
    }
    
    /**
     * Search fee collections
     * CHỈ LẤY CÁC HỘ CÓ CHỦ HỘ (relationship = 'Chủ hộ')
     * Đảm bảo mỗi household chỉ có 1 chủ hộ được hiển thị
     * Lấy owner_name từ residents table để đảm bảo đồng bộ
     */
    public List<FeeCollection> search(String apartmentCode, String householdCode, String ownerName, 
                                      Integer month, Integer year, String status) throws DbException {
        List<FeeCollection> fees = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        StringBuilder sql = new StringBuilder();
        
        if (isPostgreSQL) {
            sql.append("SELECT DISTINCT ON (fc.id) fc.*, ");
        } else {
            sql.append("SELECT fc.*, ");
        }
        
        List<Object> params = new ArrayList<>();
        
        if (isPostgreSQL) {
            sql.append(
                "a.apartment_code, " +
                "h.household_code, " +
                "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                "FROM fee_collections fc " +
                "JOIN households h ON fc.household_id = h.id " +
                "JOIN apartments a ON h.apartment_id = a.id " +
                "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' "
            );
        } else {
            sql.append(
                "a.apartment_code, " +
                "h.household_code, " +
                "COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC, r.created_at DESC LIMIT 1), h.owner_name) as owner_name " +
                "FROM fee_collections fc " +
                "JOIN households h ON fc.household_id = h.id " +
                "JOIN apartments a ON h.apartment_id = a.id " +
                "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' "
            );
        }
        
        // Thêm điều kiện tìm kiếm theo tên chủ hộ - filter trên calculated owner_name
        // Sử dụng HAVING hoặc subquery để filter trên calculated field
        if (ownerName != null && !ownerName.trim().isEmpty()) {
            String searchPattern = "%" + ownerName.trim() + "%";
            if (isPostgreSQL) {
                // Filter trên COALESCE result bằng cách dùng subquery trong WHERE
                sql.append(" AND (COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC NULLS LAST, r.created_at DESC LIMIT 1), h.owner_name) ILIKE ?)");
            } else {
                sql.append(" AND (COALESCE((SELECT r.full_name FROM residents r WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' ORDER BY r.user_id DESC, r.created_at DESC LIMIT 1), h.owner_name) LIKE ?)");
            }
            params.add(searchPattern);
        }
        
        // Các điều kiện tìm kiếm khác
        if (apartmentCode != null && !apartmentCode.trim().isEmpty()) {
            if (isPostgreSQL) {
                sql.append(" AND a.apartment_code ILIKE ?");
            } else {
                sql.append(" AND a.apartment_code LIKE ?");
            }
            params.add("%" + apartmentCode.trim() + "%");
        }
        if (householdCode != null && !householdCode.trim().isEmpty()) {
            if (isPostgreSQL) {
                sql.append(" AND h.household_code ILIKE ?");
            } else {
                sql.append(" AND h.household_code LIKE ?");
            }
            params.add("%" + householdCode.trim() + "%");
        }
        if (month != null) {
            sql.append(" AND fc.month = ?");
            params.add(month);
        }
        if (year != null) {
            sql.append(" AND fc.year = ?");
            params.add(year);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND fc.status = ?");
            params.add(status);
        }
        
        if (isPostgreSQL) {
            sql.append(" ORDER BY fc.id, fc.year DESC, fc.month DESC, fc.created_at DESC");
        } else {
            sql.append(" ORDER BY fc.year DESC, fc.month DESC, fc.created_at DESC");
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            // Debug log
            System.out.println("DEBUG: FeeCollectionRepository.search()");
            System.out.println("  SQL: " + sql.toString());
            System.out.println("  Parameters: " + params);
            System.out.println("  apartmentCode: " + apartmentCode);
            System.out.println("  householdCode: " + householdCode);
            System.out.println("  ownerName: " + ownerName);
            System.out.println("  month: " + month);
            System.out.println("  year: " + year);
            System.out.println("  status: " + status);
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                fees.add(mapResultSetToFeeCollection(rs));
                count++;
                if (count <= 5) {
                    System.out.println("  Found fee #" + count + ": id=" + rs.getInt("id") + 
                                     ", owner_name=" + rs.getString("owner_name") + 
                                     ", status=" + rs.getString("status"));
                }
            }
            System.out.println("  Total found: " + count);
        } catch (SQLException e) {
            System.err.println("ERROR in search: " + e.getMessage());
            e.printStackTrace();
            throw new DbException("Error searching fee collections: " + e.getMessage(), e);
        }
        return fees;
    }
    
    /**
     * Create fee collection
     */
    public FeeCollection create(FeeCollection fee) throws DbException {
        // Kiểm tra xem database có các cột mới chưa
        boolean hasFeeTypeColumn = checkColumnExists("fee_collections", "fee_type");
        boolean hasFeeTypeIdColumn = checkColumnExists("fee_collections", "fee_type_id");
        
        StringBuilder sql = new StringBuilder("INSERT INTO fee_collections (household_id, month, year, amount, paid_amount, status, ");
        List<String> columns = new ArrayList<>();
        
        if (hasFeeTypeColumn) {
            columns.add("fee_type");
        }
        if (hasFeeTypeIdColumn && fee.getFeeTypeId() != null) {
            columns.add("fee_type_id");
        }
        if (hasFeeTypeColumn) {
            columns.add("reason");
        }
        columns.add("payment_date");
        boolean hasPaymentDeadlineColumn = checkColumnExists("fee_collections", "payment_deadline");
        if (hasPaymentDeadlineColumn) {
            columns.add("payment_deadline");
        }
        columns.add("payment_method");
        columns.add("notes");
        
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (?, ?, ?, ?, ?, ?");
        for (int i = 0; i < columns.size(); i++) {
            sql.append(", ?");
        }
        sql.append(") RETURNING id");
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            stmt.setInt(paramIndex++, fee.getHouseholdId());
            
            if (fee.getMonth() != null) {
                stmt.setInt(paramIndex++, fee.getMonth());
            } else {
                stmt.setNull(paramIndex++, Types.INTEGER);
            }
            
            if (fee.getYear() != null) {
                stmt.setInt(paramIndex++, fee.getYear());
            } else {
                stmt.setNull(paramIndex++, Types.INTEGER);
            }
            
            stmt.setBigDecimal(paramIndex++, fee.getAmount());
            stmt.setBigDecimal(paramIndex++, fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO);
            stmt.setString(paramIndex++, fee.getStatus() != null ? fee.getStatus() : "unpaid");
            
            // Set các cột optional
            if (hasFeeTypeColumn) {
                stmt.setString(paramIndex++, fee.getFeeType() != null ? fee.getFeeType() : "periodic");
            }
            if (hasFeeTypeIdColumn && fee.getFeeTypeId() != null) {
                stmt.setInt(paramIndex++, fee.getFeeTypeId());
            }
            if (hasFeeTypeColumn) {
                stmt.setString(paramIndex++, fee.getReason());
            }
            
            if (fee.getPaymentDate() != null) {
                stmt.setDate(paramIndex++, Date.valueOf(fee.getPaymentDate()));
            } else {
                stmt.setNull(paramIndex++, Types.DATE);
            }
            
            if (hasPaymentDeadlineColumn) {
                if (fee.getPaymentDeadline() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(fee.getPaymentDeadline()));
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                }
            }
            
            stmt.setString(paramIndex++, fee.getPaymentMethod());
            stmt.setString(paramIndex++, fee.getNotes());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                fee.setId(rs.getInt(1));
            }
            return fee;
        } catch (SQLException e) {
            throw new DbException("Error creating fee collection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Kiểm tra xem cột có tồn tại trong bảng không
     */
    private boolean checkColumnExists(String tableName, String columnName) {
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM information_schema.columns " +
                 "WHERE table_name = ? AND column_name = ?")) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            // Nếu không kiểm tra được, giả sử cột không tồn tại
            return false;
        }
        return false;
    }
    
    /**
     * Update fee collection
     */
    public void update(FeeCollection fee) throws DbException {
        boolean hasPaymentDeadlineColumn = checkColumnExists("fee_collections", "payment_deadline");
        
        StringBuilder sql = new StringBuilder("UPDATE fee_collections SET amount = ?, paid_amount = ?, status = ?, fee_type = ?, reason = ?, ");
        if (hasPaymentDeadlineColumn) {
            sql.append("payment_deadline = ?, ");
        }
        sql.append("payment_date = ?, payment_method = ?, notes = ? WHERE id = ?");
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            stmt.setBigDecimal(paramIndex++, fee.getAmount());
            stmt.setBigDecimal(paramIndex++, fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO);
            stmt.setString(paramIndex++, fee.getStatus());
            stmt.setString(paramIndex++, fee.getFeeType() != null ? fee.getFeeType() : "periodic");
            stmt.setString(paramIndex++, fee.getReason());
            
            if (hasPaymentDeadlineColumn) {
                if (fee.getPaymentDeadline() != null) {
                    stmt.setDate(paramIndex++, Date.valueOf(fee.getPaymentDeadline()));
                } else {
                    stmt.setNull(paramIndex++, Types.DATE);
                }
            }
            
            if (fee.getPaymentDate() != null) {
                stmt.setDate(paramIndex++, Date.valueOf(fee.getPaymentDate()));
            } else {
                stmt.setNull(paramIndex++, Types.DATE);
            }
            
            stmt.setString(paramIndex++, fee.getPaymentMethod());
            stmt.setString(paramIndex++, fee.getNotes());
            stmt.setInt(paramIndex++, fee.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating fee collection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark as paid
     */
    public void markAsPaid(Integer id, LocalDate paymentDate, String paymentMethod) throws DbException {
        // First get the fee to get the amount
        FeeCollection fee = findById(id);
        if (fee == null) {
            throw new DbException("Fee collection not found");
        }
        
        // Update status to paid and set paid_amount = amount
        String sql = "UPDATE fee_collections SET status = 'paid', paid_amount = amount, payment_date = ?, " +
                     "payment_method = ? WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(paymentDate));
            stmt.setString(2, paymentMethod);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error marking fee as paid: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete fee collection by ID
     */
    public void delete(Integer id) throws DbException {
        String sql = "DELETE FROM fee_collections WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DbException("Fee collection not found");
            }
        } catch (SQLException e) {
            throw new DbException("Error deleting fee collection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete all fee collections for a household
     */
    public void deleteByHouseholdId(Integer householdId) throws DbException {
        String sql = "DELETE FROM fee_collections WHERE household_id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting fee collections by household: " + e.getMessage(), e);
        }
    }
    
    private FeeCollection mapResultSetToFeeCollection(ResultSet rs) throws SQLException {
        FeeCollection fee = new FeeCollection();
        fee.setId(rs.getInt("id"));
        fee.setHouseholdId(rs.getInt("household_id"));
        
        // Month và year có thể NULL cho thu phí không định kỳ
        int month = rs.getInt("month");
        if (!rs.wasNull()) {
            fee.setMonth(month);
        }
        
        int year = rs.getInt("year");
        if (!rs.wasNull()) {
            fee.setYear(year);
        }
        
        BigDecimal amount = rs.getBigDecimal("amount");
        fee.setAmount(amount != null ? amount : BigDecimal.ZERO);
        
        BigDecimal paidAmount = rs.getBigDecimal("paid_amount");
        fee.setPaidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO);
        
        fee.setStatus(rs.getString("status"));
        
        // Fee type và reason
        try {
            fee.setFeeType(rs.getString("fee_type"));
        } catch (SQLException e) {
            // Column might not exist in older databases
            fee.setFeeType("periodic");
        }
        
        try {
            fee.setReason(rs.getString("reason"));
        } catch (SQLException e) {
            // Column might not exist in older databases
            fee.setReason(null);
        }
        
        Date paymentDate = rs.getDate("payment_date");
        if (paymentDate != null) {
            fee.setPaymentDate(paymentDate.toLocalDate());
        }
        
        try {
            Date paymentDeadline = rs.getDate("payment_deadline");
            if (paymentDeadline != null) {
                fee.setPaymentDeadline(paymentDeadline.toLocalDate());
            }
        } catch (SQLException e) {
            // Column might not exist in older databases
            fee.setPaymentDeadline(null);
        }
        
        fee.setPaymentMethod(rs.getString("payment_method"));
        fee.setNotes(rs.getString("notes"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            fee.setCreatedAt(createdAt.toLocalDateTime().toLocalDate());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            fee.setUpdatedAt(updatedAt.toLocalDateTime().toLocalDate());
        }
        
        // Join fields
        fee.setApartmentCode(rs.getString("apartment_code"));
        fee.setHouseholdCode(rs.getString("household_code"));
        fee.setOwnerName(rs.getString("owner_name"));
        
        return fee;
    }
    
    /**
     * Check if using PostgreSQL
     */
    private boolean isPostgreSQL() {
        try {
            String driver = vn.bluemoon.config.DbConfig.getInstance().getDriver();
            return driver != null && driver.contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }
}


