package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeType;
import vn.bluemoon.util.JdbcUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for FeeType entity
 */
public class FeeTypeRepository {
    
    /**
     * Find all fee types
     */
    public List<FeeType> findAll() throws DbException {
        List<FeeType> feeTypes = new ArrayList<>();
        String sql = "SELECT * FROM fee_types ORDER BY name";
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                feeTypes.add(mapResultSetToFeeType(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all fee types: " + e.getMessage(), e);
        }
        return feeTypes;
    }
    
    /**
     * Find active fee types only
     */
    public List<FeeType> findActive() throws DbException {
        List<FeeType> feeTypes = new ArrayList<>();
        String sql = "SELECT * FROM fee_types WHERE is_active = TRUE ORDER BY name";
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                feeTypes.add(mapResultSetToFeeType(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding active fee types: " + e.getMessage(), e);
        }
        return feeTypes;
    }
    
    /**
     * Find fee type by ID
     */
    public FeeType findById(Integer id) throws DbException {
        String sql = "SELECT * FROM fee_types WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFeeType(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding fee type by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create new fee type
     */
    public Integer create(FeeType feeType) throws DbException {
        String sql = "INSERT INTO fee_types (name, description, default_amount, is_active) " +
                     "VALUES (?, ?, ?, ?) RETURNING id";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, feeType.getName());
            stmt.setString(2, feeType.getDescription());
            stmt.setBigDecimal(3, feeType.getDefaultAmount() != null ? feeType.getDefaultAmount() : BigDecimal.ZERO);
            stmt.setBoolean(4, feeType.getIsActive() != null ? feeType.getIsActive() : true);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new DbException("Failed to create fee type");
        } catch (SQLException e) {
            throw new DbException("Error creating fee type: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update fee type
     */
    public void update(FeeType feeType) throws DbException {
        String sql = "UPDATE fee_types SET " +
                     "name = ?, description = ?, default_amount = ?, is_active = ?, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, feeType.getName());
            stmt.setString(2, feeType.getDescription());
            stmt.setBigDecimal(3, feeType.getDefaultAmount() != null ? feeType.getDefaultAmount() : BigDecimal.ZERO);
            stmt.setBoolean(4, feeType.getIsActive() != null ? feeType.getIsActive() : true);
            stmt.setInt(5, feeType.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating fee type: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete fee type (soft delete by setting is_active = false)
     */
    public void delete(Integer id) throws DbException {
        String sql = "UPDATE fee_types SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting fee type: " + e.getMessage(), e);
        }
    }
    
    private FeeType mapResultSetToFeeType(ResultSet rs) throws SQLException {
        FeeType feeType = new FeeType();
        feeType.setId(rs.getInt("id"));
        feeType.setName(rs.getString("name"));
        feeType.setDescription(rs.getString("description"));
        
        BigDecimal amount = rs.getBigDecimal("default_amount");
        if (amount != null) {
            feeType.setDefaultAmount(amount);
        }
        
        feeType.setIsActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            feeType.setCreatedAt(createdAt.toLocalDateTime().toLocalDate());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            feeType.setUpdatedAt(updatedAt.toLocalDateTime().toLocalDate());
        }
        
        return feeType;
    }
}





