package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Resident entity
 */
public class ResidentRepository {
    
    /**
     * Find all residents with household and apartment info
     * CHỈ LẤY CHỦ HỘ (relationship = 'Chủ hộ')
     * Lấy tất cả households, sau đó tìm resident tương ứng (nếu có)
     */
    public List<Resident> findAll() throws DbException {
        List<Resident> residents = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        
        System.out.println("DEBUG: ResidentRepository.findAll() - isPostgreSQL=" + isPostgreSQL);
        
        // Lấy tất cả households có ít nhất 1 resident với relationship = 'Chủ hộ'
        // Ưu tiên resident có user_id (resident được liên kết với user account)
        String sql;
        if (isPostgreSQL) {
            sql = "SELECT " +
                 "r.id, " +
                 "h.id as household_id, " +
                 "r.user_id, " +
                 "COALESCE(r.full_name, h.owner_name) as full_name, " +
                 "COALESCE(r.id_card, h.owner_id_card) as id_card, " +
                 "r.date_of_birth, " +
                 "r.gender, " +
                 "COALESCE(r.relationship, 'Chủ hộ') as relationship, " +
                 "COALESCE(r.phone, h.owner_phone) as phone, " +
                 "COALESCE(r.email, h.owner_email) as email, " +
                 "r.occupation, " +
                 "r.permanent_address, " +
                 "r.temporary_address, " +
                 "r.status, " +
                 "r.notes, " +
                 "r.temporary_resident_from, " +
                 "r.temporary_resident_to, " +
                 "r.temporary_absent_from, " +
                 "r.temporary_absent_to, " +
                 "r.temporary_reason, " +
                 "COALESCE(r.created_at, h.created_at) as created_at, " +
                 "COALESCE(r.updated_at, h.updated_at) as updated_at, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "h.owner_name " +
                 "FROM households h " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "INNER JOIN LATERAL ( " +
                 "    SELECT r.* FROM residents r " +
                 "    WHERE r.household_id = h.id AND r.relationship = 'Chủ hộ' " +
                 "    ORDER BY CASE WHEN r.user_id IS NOT NULL THEN 0 ELSE 1 END, " +
                 "             r.user_id DESC NULLS LAST, r.created_at DESC " +
                 "    LIMIT 1 " +
                 ") r ON true " +
                 "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY h.id";
        } else {
            // MySQL - chỉ lấy households có ít nhất 1 resident với relationship = 'Chủ hộ'
            // Dùng INNER JOIN để đảm bảo chỉ lấy households có residents
            sql = "SELECT " +
                 "r.id, " +
                 "h.id as household_id, " +
                 "r.user_id, " +
                 "COALESCE(r.full_name, h.owner_name) as full_name, " +
                 "COALESCE(r.id_card, h.owner_id_card) as id_card, " +
                 "r.date_of_birth, " +
                 "r.gender, " +
                 "COALESCE(r.relationship, 'Chủ hộ') as relationship, " +
                 "COALESCE(r.phone, h.owner_phone) as phone, " +
                 "COALESCE(r.email, h.owner_email) as email, " +
                 "r.occupation, " +
                 "r.permanent_address, " +
                 "r.temporary_address, " +
                 "r.status, " +
                 "r.notes, " +
                 "r.temporary_resident_from, " +
                 "r.temporary_resident_to, " +
                 "r.temporary_absent_from, " +
                 "r.temporary_absent_to, " +
                 "r.temporary_reason, " +
                 "COALESCE(r.created_at, h.created_at) as created_at, " +
                 "COALESCE(r.updated_at, h.updated_at) as updated_at, " +
                 "a.apartment_code, " +
                 "h.household_code, " +
                 "h.owner_name " +
                 "FROM households h " +
                 "JOIN apartments a ON h.apartment_id = a.id " +
                 "INNER JOIN residents r ON r.household_id = h.id " +
                 "AND r.relationship = 'Chủ hộ' " +
                 "AND r.id = (SELECT r2.id FROM residents r2 " +
                 "            WHERE r2.household_id = h.id " +
                 "            AND r2.relationship = 'Chủ hộ' " +
                 "            ORDER BY CASE WHEN r2.user_id IS NOT NULL THEN 0 ELSE 1 END, " +
                 "                     r2.user_id DESC, r2.created_at DESC " +
                 "            LIMIT 1) " +
                 "WHERE a.apartment_code NOT LIKE 'DEFAULT-%' " +
                 "ORDER BY h.id";
        }
        
        System.out.println("DEBUG: Executing SQL: " + sql);
        
        // Dùng Set để loại bỏ duplicate households theo household_id
        java.util.Set<Integer> seenHouseholdIds = new java.util.HashSet<>();
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            int duplicateCount = 0;
            while (rs.next()) {
                Resident resident = mapResultSetToResident(rs);
                Integer householdId = resident.getHouseholdId();
                
                // Chỉ thêm nếu chưa thấy household_id này
                if (!seenHouseholdIds.contains(householdId)) {
                    seenHouseholdIds.add(householdId);
                    residents.add(resident);
                    count++;
                    System.out.println("DEBUG: Found household #" + count + ": " + resident.getFullName() + 
                                     " (Resident ID: " + resident.getId() + ", Household ID: " + householdId + 
                                     ", Apartment: " + resident.getApartmentCode() + ", Household Code: " + resident.getHouseholdCode() + ")");
                } else {
                    duplicateCount++;
                    System.out.println("DEBUG: Skipping duplicate household ID: " + householdId + " (Resident: " + resident.getFullName() + ")");
                }
            }
            System.out.println("DEBUG: Total unique households found: " + count + " (duplicates skipped: " + duplicateCount + ")");
        } catch (SQLException e) {
            System.err.println("DEBUG: SQL Error: " + e.getMessage());
            e.printStackTrace();
            throw new DbException("Error finding all residents: " + e.getMessage(), e);
        }
        return residents;
    }
    
    /**
     * Check if database is PostgreSQL
     */
    private boolean isPostgreSQL() {
        try {
            String driver = vn.bluemoon.config.DbConfig.getInstance().getDriver();
            return driver != null && driver.contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Search residents by name, apartment code, or household code
     * CHỈ LẤY CHỦ HỘ (relationship = 'Chủ hộ')
     */
    public List<Resident> search(String name, String apartmentCode, String householdCode) throws DbException {
        List<Resident> residents = new ArrayList<>();
        boolean isPostgreSQL = isPostgreSQL();
        
        // Đảm bảo mỗi household chỉ có một chủ hộ (ưu tiên resident có user_id)
        StringBuilder sql;
        if (isPostgreSQL) {
            sql = new StringBuilder(
                "SELECT DISTINCT ON (h.id) " +
                "r.*, " +
                "a.apartment_code, " +
                "h.household_code, " +
                "h.owner_name " +
                "FROM households h " +
                "JOIN apartments a ON h.apartment_id = a.id " +
                "INNER JOIN LATERAL ( " +
                "    SELECT r2.* FROM residents r2 " +
                "    WHERE r2.household_id = h.id AND r2.relationship = 'Chủ hộ' " +
                "    ORDER BY CASE WHEN r2.user_id IS NOT NULL THEN 0 ELSE 1 END, " +
                "             r2.user_id DESC NULLS LAST, r2.created_at DESC " +
                "    LIMIT 1 " +
                ") r ON true " +
                "WHERE a.apartment_code NOT LIKE 'DEFAULT-%'"
            );
        } else {
            sql = new StringBuilder(
                "SELECT r.*, " +
                "a.apartment_code, " +
                "h.household_code, " +
                "h.owner_name " +
                "FROM households h " +
                "JOIN apartments a ON h.apartment_id = a.id " +
                "INNER JOIN residents r ON r.household_id = h.id " +
                "AND r.relationship = 'Chủ hộ' " +
                "AND r.id = (SELECT r2.id FROM residents r2 " +
                "            WHERE r2.household_id = h.id " +
                "            AND r2.relationship = 'Chủ hộ' " +
                "            ORDER BY CASE WHEN r2.user_id IS NOT NULL THEN 0 ELSE 1 END, " +
                "                     r2.user_id DESC, r2.created_at DESC " +
                "            LIMIT 1) " +
                "WHERE a.apartment_code NOT LIKE 'DEFAULT-%'"
            );
        }
        
        List<Object> params = new ArrayList<>();
        
        if (name != null && !name.trim().isEmpty()) {
            if (isPostgreSQL) {
                // Tìm trong cả r.full_name và h.owner_name
                sql.append(" AND (COALESCE(r.full_name, h.owner_name) ILIKE ? OR h.owner_name ILIKE ?)");
            } else {
                sql.append(" AND (COALESCE(r.full_name, h.owner_name) LIKE ? OR h.owner_name LIKE ?)");
            }
            params.add("%" + name + "%");
            params.add("%" + name + "%");
        }
        if (apartmentCode != null && !apartmentCode.trim().isEmpty()) {
            if (isPostgreSQL) {
                sql.append(" AND a.apartment_code ILIKE ?");
            } else {
                sql.append(" AND a.apartment_code LIKE ?");
            }
            params.add("%" + apartmentCode + "%");
        }
        if (householdCode != null && !householdCode.trim().isEmpty()) {
            if (isPostgreSQL) {
                sql.append(" AND h.household_code ILIKE ?");
            } else {
                sql.append(" AND h.household_code LIKE ?");
            }
            params.add("%" + householdCode + "%");
        }
        
        if (isPostgreSQL) {
            sql.append(" ORDER BY h.id, r.user_id DESC NULLS LAST, r.created_at DESC");
        } else {
            sql.append(" ORDER BY h.id");
        }
        
        System.out.println("DEBUG: ResidentRepository.search() - name=" + name + ", apartmentCode=" + apartmentCode + ", householdCode=" + householdCode);
        System.out.println("DEBUG: SQL=" + sql.toString());
        System.out.println("DEBUG: Params=" + params);
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                Resident resident = mapResultSetToResident(rs);
                residents.add(resident);
                count++;
                System.out.println("DEBUG: Found resident #" + count + ": " + resident.getFullName() + 
                                 " (Resident ID: " + resident.getId() + ", Household ID: " + resident.getHouseholdId() + 
                                 ", User ID: " + resident.getUserId() + ", Apartment: " + resident.getApartmentCode() + ")");
            }
            System.out.println("DEBUG: Total residents found: " + count);
        } catch (SQLException e) {
            System.err.println("DEBUG: SQL Error in search: " + e.getMessage());
            e.printStackTrace();
            throw new DbException("Error searching residents: " + e.getMessage(), e);
        }
        return residents;
    }
    
    /**
     * Find resident by ID
     */
    public Resident findById(Integer id) throws DbException {
        String sql = "SELECT r.*, " +
                     "a.apartment_code, " +
                     "h.household_code, " +
                     "h.owner_name " +
                     "FROM residents r " +
                     "JOIN households h ON r.household_id = h.id " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE r.id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToResident(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding resident by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find resident by user_id
     */
    public Resident findByUserId(Integer userId) throws DbException {
        String sql = "SELECT r.*, " +
                     "a.apartment_code, " +
                     "h.household_code, " +
                     "h.owner_name " +
                     "FROM residents r " +
                     "JOIN households h ON r.household_id = h.id " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE r.user_id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToResident(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding resident by user_id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create new resident
     */
    public Integer create(Resident resident) throws DbException {
        String sql = "INSERT INTO residents (household_id, user_id, full_name, id_card, date_of_birth, " +
                     "gender, relationship, phone, email, occupation, permanent_address, temporary_address, status, " +
                     "temporary_resident_from, temporary_resident_to, temporary_absent_from, temporary_absent_to, temporary_reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, resident.getHouseholdId());
            if (resident.getUserId() != null) {
                stmt.setInt(2, resident.getUserId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, resident.getFullName());
            stmt.setString(4, resident.getIdCard());
            
            if (resident.getDateOfBirth() != null) {
                stmt.setDate(5, Date.valueOf(resident.getDateOfBirth()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            
            stmt.setString(6, resident.getGender());
            stmt.setString(7, resident.getRelationship());
            stmt.setString(8, resident.getPhone());
            stmt.setString(9, resident.getEmail());
            stmt.setString(10, resident.getOccupation());
            stmt.setString(11, resident.getPermanentAddress());
            stmt.setString(12, resident.getTemporaryAddress());
            stmt.setString(13, resident.getStatus() != null ? resident.getStatus() : "active");
            
            // Tạm trú/Tạm vắng
            if (resident.getTemporaryResidentFrom() != null) {
                stmt.setDate(14, Date.valueOf(resident.getTemporaryResidentFrom()));
            } else {
                stmt.setNull(14, Types.DATE);
            }
            if (resident.getTemporaryResidentTo() != null) {
                stmt.setDate(15, Date.valueOf(resident.getTemporaryResidentTo()));
            } else {
                stmt.setNull(15, Types.DATE);
            }
            if (resident.getTemporaryAbsentFrom() != null) {
                stmt.setDate(16, Date.valueOf(resident.getTemporaryAbsentFrom()));
            } else {
                stmt.setNull(16, Types.DATE);
            }
            if (resident.getTemporaryAbsentTo() != null) {
                stmt.setDate(17, Date.valueOf(resident.getTemporaryAbsentTo()));
            } else {
                stmt.setNull(17, Types.DATE);
            }
            stmt.setString(18, resident.getTemporaryReason());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new DbException("Failed to create resident");
        } catch (SQLException e) {
            throw new DbException("Error creating resident: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update resident
     */
    public void update(Resident resident) throws DbException {
        String sql = "UPDATE residents SET " +
                     "household_id = ?, full_name = ?, id_card = ?, date_of_birth = ?, " +
                     "gender = ?, relationship = ?, phone = ?, email = ?, occupation = ?, " +
                     "permanent_address = ?, temporary_address = ?, status = ?, " +
                     "temporary_resident_from = ?, temporary_resident_to = ?, " +
                     "temporary_absent_from = ?, temporary_absent_to = ?, temporary_reason = ?, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, resident.getHouseholdId());
            stmt.setString(2, resident.getFullName());
            stmt.setString(3, resident.getIdCard());
            
            if (resident.getDateOfBirth() != null) {
                stmt.setDate(4, Date.valueOf(resident.getDateOfBirth()));
            } else {
                stmt.setNull(4, Types.DATE);
            }
            
            stmt.setString(5, resident.getGender());
            stmt.setString(6, resident.getRelationship());
            stmt.setString(7, resident.getPhone());
            stmt.setString(8, resident.getEmail());
            stmt.setString(9, resident.getOccupation());
            stmt.setString(10, resident.getPermanentAddress());
            stmt.setString(11, resident.getTemporaryAddress());
            stmt.setString(12, resident.getStatus() != null ? resident.getStatus() : "active");
            
            // Tạm trú/Tạm vắng
            if (resident.getTemporaryResidentFrom() != null) {
                stmt.setDate(13, Date.valueOf(resident.getTemporaryResidentFrom()));
            } else {
                stmt.setNull(13, Types.DATE);
            }
            if (resident.getTemporaryResidentTo() != null) {
                stmt.setDate(14, Date.valueOf(resident.getTemporaryResidentTo()));
            } else {
                stmt.setNull(14, Types.DATE);
            }
            if (resident.getTemporaryAbsentFrom() != null) {
                stmt.setDate(15, Date.valueOf(resident.getTemporaryAbsentFrom()));
            } else {
                stmt.setNull(15, Types.DATE);
            }
            if (resident.getTemporaryAbsentTo() != null) {
                stmt.setDate(16, Date.valueOf(resident.getTemporaryAbsentTo()));
            } else {
                stmt.setNull(16, Types.DATE);
            }
            stmt.setString(17, resident.getTemporaryReason());
            
            stmt.setInt(18, resident.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating resident: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete resident by ID
     */
    public void delete(Integer residentId) throws DbException {
        String sql = "DELETE FROM residents WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting resident: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if household has any other residents
     */
    public boolean hasOtherResidents(Integer householdId, Integer excludeResidentId) throws DbException {
        String sql = "SELECT COUNT(*) FROM residents WHERE household_id = ? AND id != ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            stmt.setInt(2, excludeResidentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking other residents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Count all residents (only owners - relationship = 'Chủ hộ')
     */
    public int countAll() throws DbException {
        String sql = "SELECT COUNT(*) as count FROM residents WHERE relationship = 'Chủ hộ'";
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            throw new DbException("Error counting residents: " + e.getMessage(), e);
        }
        return 0;
    }
    
    private Resident mapResultSetToResident(ResultSet rs) throws SQLException {
        Resident resident = new Resident();
        resident.setId(rs.getInt("id"));
        resident.setHouseholdId(rs.getInt("household_id"));
        
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            resident.setUserId(userId);
        }
        
        resident.setFullName(rs.getString("full_name"));
        resident.setIdCard(rs.getString("id_card"));
        
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            resident.setDateOfBirth(dob.toLocalDate());
        }
        
        resident.setGender(rs.getString("gender"));
        resident.setRelationship(rs.getString("relationship"));
        resident.setPhone(rs.getString("phone"));
        resident.setEmail(rs.getString("email"));
        resident.setOccupation(rs.getString("occupation"));
        resident.setPermanentAddress(rs.getString("permanent_address"));
        resident.setTemporaryAddress(rs.getString("temporary_address"));
        resident.setStatus(rs.getString("status"));
        resident.setNotes(rs.getString("notes"));
        
        // Tạm trú/Tạm vắng
        Date tempResFrom = rs.getDate("temporary_resident_from");
        if (tempResFrom != null) {
            resident.setTemporaryResidentFrom(tempResFrom.toLocalDate());
        }
        Date tempResTo = rs.getDate("temporary_resident_to");
        if (tempResTo != null) {
            resident.setTemporaryResidentTo(tempResTo.toLocalDate());
        }
        Date tempAbsFrom = rs.getDate("temporary_absent_from");
        if (tempAbsFrom != null) {
            resident.setTemporaryAbsentFrom(tempAbsFrom.toLocalDate());
        }
        Date tempAbsTo = rs.getDate("temporary_absent_to");
        if (tempAbsTo != null) {
            resident.setTemporaryAbsentTo(tempAbsTo.toLocalDate());
        }
        resident.setTemporaryReason(rs.getString("temporary_reason"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            resident.setCreatedAt(createdAt.toLocalDateTime().toLocalDate());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            resident.setUpdatedAt(updatedAt.toLocalDateTime().toLocalDate());
        }
        
        // Join fields
        resident.setApartmentCode(rs.getString("apartment_code"));
        resident.setHouseholdCode(rs.getString("household_code"));
        resident.setOwnerName(rs.getString("owner_name"));
        
        return resident;
    }
}

