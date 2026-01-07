package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.repository.FeeCollectionRepository;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for Resident management
 */
public class ResidentService {
    private final ResidentRepository residentRepository = new ResidentRepository();
    private final FeeCollectionRepository feeCollectionRepository = new FeeCollectionRepository();
    
    /**
     * Get all residents
     */
    public List<Resident> getAllResidents() throws DbException {
        return residentRepository.findAll();
    }
    
    /**
     * Search residents
     */
    public List<Resident> searchResidents(String name, String apartmentCode, String householdCode) throws DbException {
        return residentRepository.search(name, apartmentCode, householdCode);
    }
    
    /**
     * Get resident by ID
     */
    public Resident getResidentById(Integer id) throws DbException {
        return residentRepository.findById(id);
    }
    
    /**
     * Delete resident and related data
     * - If resident is "Chủ hộ", delete all fee collections for the household
     * - Delete the resident
     * - If household has no other residents, delete the household
     */
    public void deleteResident(Integer residentId) throws DbException {
        // Get resident info before deleting
        Resident resident = residentRepository.findById(residentId);
        if (resident == null) {
            throw new DbException("Không tìm thấy nhân khẩu");
        }
        
        Integer householdId = resident.getHouseholdId();
        boolean isChuHo = "Chủ hộ".equals(resident.getRelationship());
        
        // If resident is "Chủ hộ", delete all fee collections for the household
        if (isChuHo) {
            feeCollectionRepository.deleteByHouseholdId(householdId);
        }
        
        // Delete the resident
        residentRepository.delete(residentId);
        
        // Check if household has any other residents
        boolean hasOtherResidents = residentRepository.hasOtherResidents(householdId, residentId);
        
        // If no other residents, delete the household
        if (!hasOtherResidents) {
            deleteHousehold(householdId);
        }
    }
    
    /**
     * Delete household by ID
     */
    private void deleteHousehold(Integer householdId) throws DbException {
        String sql = "DELETE FROM households WHERE id = ?";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new DbException("Error deleting household: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đăng ký tạm trú cho cư dân
     */
    public void registerTemporaryResident(Integer residentId, LocalDate fromDate, LocalDate toDate, String reason) 
            throws DbException, ValidationException {
        if (fromDate == null) {
            throw new ValidationException("Ngày bắt đầu tạm trú không được để trống");
        }
        if (toDate == null) {
            throw new ValidationException("Ngày kết thúc tạm trú không được để trống");
        }
        
        if (toDate.isBefore(fromDate)) {
            throw new ValidationException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        Resident resident = residentRepository.findById(residentId);
        if (resident == null) {
            throw new ValidationException("Không tìm thấy cư dân");
        }
        
        resident.setStatus("temporary_resident");
        resident.setTemporaryResidentFrom(fromDate);
        resident.setTemporaryResidentTo(toDate);
        resident.setTemporaryReason(reason);
        // Clear tạm vắng nếu có
        resident.setTemporaryAbsentFrom(null);
        resident.setTemporaryAbsentTo(null);
        
        residentRepository.update(resident);
    }
    
    /**
     * Đăng ký tạm vắng cho cư dân
     */
    public void registerTemporaryAbsent(Integer residentId, LocalDate fromDate, LocalDate toDate, String reason) 
            throws DbException, ValidationException {
        if (fromDate == null) {
            throw new ValidationException("Ngày bắt đầu tạm vắng không được để trống");
        }
        if (toDate == null) {
            throw new ValidationException("Ngày kết thúc tạm vắng không được để trống");
        }
        
        if (toDate.isBefore(fromDate)) {
            throw new ValidationException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        Resident resident = residentRepository.findById(residentId);
        if (resident == null) {
            throw new ValidationException("Không tìm thấy cư dân");
        }
        
        resident.setStatus("temporary_absent");
        resident.setTemporaryAbsentFrom(fromDate);
        resident.setTemporaryAbsentTo(toDate);
        resident.setTemporaryReason(reason);
        // Clear tạm trú nếu có
        resident.setTemporaryResidentFrom(null);
        resident.setTemporaryResidentTo(null);
        
        residentRepository.update(resident);
    }
    
    /**
     * Hủy tạm trú/tạm vắng, trở về trạng thái active
     */
    public void cancelTemporaryStatus(Integer residentId) throws DbException, ValidationException {
        Resident resident = residentRepository.findById(residentId);
        if (resident == null) {
            throw new ValidationException("Không tìm thấy cư dân");
        }
        
        resident.setStatus("active");
        resident.setTemporaryResidentFrom(null);
        resident.setTemporaryResidentTo(null);
        resident.setTemporaryAbsentFrom(null);
        resident.setTemporaryAbsentTo(null);
        resident.setTemporaryReason(null);
        
        residentRepository.update(resident);
    }
    
    /**
     * Tạo nhân khẩu mới (Admin/Tổ trưởng có thể thêm nhân khẩu)
     * @param householdId ID của hộ dân
     * @param fullName Họ và tên
     * @param idCard CMND/CCCD
     * @param dateOfBirth Ngày sinh
     * @param gender Giới tính (male/female)
     * @param relationship Quan hệ với chủ hộ (Chủ hộ, Vợ/Chồng, Con, ...)
     * @param phone Số điện thoại
     * @param email Email
     * @param occupation Nghề nghiệp
     * @param permanentAddress Địa chỉ thường trú
     * @param temporaryAddress Địa chỉ tạm trú
     * @param status Trạng thái (active, temporary_resident, temporary_absent, ...)
     * @return ID của nhân khẩu vừa tạo
     */
    public Integer createResident(
            Integer householdId,
            String fullName,
            String idCard,
            LocalDate dateOfBirth,
            String gender,
            String relationship,
            String phone,
            String email,
            String occupation,
            String permanentAddress,
            String temporaryAddress,
            String status) throws DbException, ValidationException {
        
        // Validate required fields
        Validators.validateRequired(fullName, "Họ và tên");
        Validators.validateRequired(idCard, "CMND/CCCD");
        if (householdId == null) {
            throw new ValidationException("Hộ dân không được để trống");
        }
        
        if (relationship == null || relationship.trim().isEmpty()) {
            relationship = "Thành viên"; // Default relationship
        }
        
        // Create resident
        Resident resident = new Resident();
        resident.setHouseholdId(householdId);
        resident.setUserId(null); // Không liên kết với user account
        resident.setFullName(fullName.trim());
        resident.setIdCard(idCard.trim());
        resident.setDateOfBirth(dateOfBirth);
        resident.setGender(gender);
        resident.setRelationship(relationship);
        resident.setPhone(phone != null ? phone.trim() : null);
        resident.setEmail(email != null ? email.trim() : null);
        resident.setOccupation(occupation != null ? occupation.trim() : null);
        resident.setPermanentAddress(permanentAddress != null ? permanentAddress.trim() : null);
        resident.setTemporaryAddress(temporaryAddress != null ? temporaryAddress.trim() : null);
        resident.setStatus(status != null ? status : "active");
        
        return residentRepository.create(resident);
    }
    
    /**
     * Tạo hộ dân mới (khi thêm chủ hộ mới)
     * @param apartmentCode Mã căn hộ
     * @param householdCode Mã hộ
     * @param ownerName Tên chủ hộ
     * @param ownerPhone Số điện thoại chủ hộ
     * @param ownerEmail Email chủ hộ
     * @return ID của hộ dân vừa tạo
     */
    public Integer createHousehold(String apartmentCode, String householdCode, String ownerName, String ownerPhone, String ownerEmail) 
            throws DbException, ValidationException {
        
        // Validate required fields
        Validators.validateRequired(apartmentCode, "Mã căn hộ");
        Validators.validateRequired(householdCode, "Mã hộ");
        Validators.validateRequired(ownerName, "Tên chủ hộ");
        
        // Check if apartment code already has a "Chủ hộ"
        boolean hasChuHo = hasChuHoInApartment(apartmentCode);
        if (hasChuHo) {
            throw new ValidationException("Mã căn hộ này đã có chủ hộ. Vui lòng chọn mã căn hộ khác.");
        }
        
        // Check if household code already exists
        Integer existingHouseholdId = findHouseholdByCode(householdCode);
        if (existingHouseholdId != null) {
            throw new ValidationException("Mã hộ này đã tồn tại. Vui lòng chọn mã hộ khác.");
        }
        
        // Find or create apartment
        Integer apartmentId = findOrCreateApartment(apartmentCode);
        
        // Create household
        String sql = "INSERT INTO households (apartment_id, household_code, owner_name, owner_phone, owner_email, " +
                     "registration_date, status) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, apartmentId);
            stmt.setString(2, householdCode.trim());
            stmt.setString(3, ownerName.trim());
            stmt.setString(4, ownerPhone != null ? ownerPhone.trim() : null);
            stmt.setString(5, ownerEmail != null ? ownerEmail.trim() : null);
            stmt.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
            stmt.setString(7, "active");
            
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new DbException("Failed to create household");
        } catch (java.sql.SQLException e) {
            // If error due to duplicate household_code, try to find it
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("household_code"))) {
                Integer existingId = findHouseholdByCode(householdCode);
                if (existingId != null) {
                    throw new ValidationException("Mã hộ này đã tồn tại. Vui lòng chọn mã hộ khác.");
                }
            }
            throw new DbException("Error creating household: " + e.getMessage(), e);
        }
    }
    
    /**
     * Kiểm tra xem apartment code đã có chủ hộ chưa
     */
    private boolean hasChuHoInApartment(String apartmentCode) throws DbException {
        String sql = "SELECT COUNT(*) FROM residents r " +
                     "JOIN households h ON r.household_id = h.id " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE a.apartment_code = ? " +
                     "AND r.relationship = 'Chủ hộ' " +
                     "AND r.status = 'active'";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apartmentCode);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (java.sql.SQLException e) {
            throw new DbException("Error checking apartment occupancy: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Tìm household theo household_code
     */
    private Integer findHouseholdByCode(String householdCode) throws DbException {
        String sql = "SELECT id FROM households WHERE household_code = ?";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, householdCode);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (java.sql.SQLException e) {
            throw new DbException("Error finding household by code: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Tìm hoặc tạo apartment theo apartment_code
     */
    private Integer findOrCreateApartment(String apartmentCode) throws DbException {
        // Try to find existing apartment
        String sql = "SELECT id FROM apartments WHERE apartment_code = ?";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apartmentCode);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (java.sql.SQLException e) {
            throw new DbException("Error finding apartment: " + e.getMessage(), e);
        }
        
        // Not found, create new apartment
        String insertSql = "INSERT INTO apartments (building_number, floor_number, room_number, apartment_code, area, number_of_rooms, status) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, "A");
            stmt.setInt(2, 1);
            stmt.setString(3, "01");
            stmt.setString(4, apartmentCode);
            stmt.setBigDecimal(5, new java.math.BigDecimal("60.00"));
            stmt.setInt(6, 2);
            stmt.setString(7, "occupied");
            
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new DbException("Failed to create apartment");
        } catch (java.sql.SQLException e) {
            // If error due to duplicate apartment_code, try to find it
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("apartment_code"))) {
                try (java.sql.Connection conn2 = vn.bluemoon.util.JdbcUtils.getConnection();
                     java.sql.PreparedStatement stmt2 = conn2.prepareStatement(sql)) {
                    stmt2.setString(1, apartmentCode);
                    java.sql.ResultSet rs2 = stmt2.executeQuery();
                    if (rs2.next()) {
                        return rs2.getInt("id");
                    }
                } catch (java.sql.SQLException e2) {
                    throw new DbException("Error finding apartment after duplicate: " + e2.getMessage(), e2);
                }
            }
            throw new DbException("Error creating apartment: " + e.getMessage(), e);
        }
    }
}


