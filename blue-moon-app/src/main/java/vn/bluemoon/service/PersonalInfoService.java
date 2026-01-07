package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.PersonalInfoRequest;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.FeeCollectionRepository;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.repository.UserRepository;
import vn.bluemoon.util.JdbcUtils;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/**
 * Service for managing personal information (đăng ký/cập nhật thông tin cá nhân)
 */
public class PersonalInfoService {
    private final ResidentRepository residentRepository = new ResidentRepository();
    private final UserRepository userRepository = new UserRepository();
    private final FeeCollectionRepository feeCollectionRepository = new FeeCollectionRepository();
    
    /**
     * Register or update personal information for a user
     */
    public void registerOrUpdatePersonalInfo(Integer userId, PersonalInfoRequest request) 
            throws DbException, ValidationException {
        
        System.out.println("=== DEBUG: registerOrUpdatePersonalInfo START ===");
        System.out.println("DEBUG: userId = " + userId);
        System.out.println("DEBUG: request.getApartmentCode() = " + request.getApartmentCode());
        System.out.println("DEBUG: request.getHouseholdCode() = " + request.getHouseholdCode());
        
        // Validate input
        Validators.validateRequired(request.getFullName(), "Họ và tên");
        Validators.validateRequired(request.getIdCard(), "CMND/CCCD");
        
        // Get user
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new ValidationException("Người dùng không tồn tại");
        }
        
        // Check if user already has resident record
        Resident existingResident = residentRepository.findByUserId(userId);
        System.out.println("DEBUG: existingResident = " + (existingResident != null ? "EXISTS" : "NULL"));
        if (existingResident != null) {
            System.out.println("DEBUG: existingResident.getApartmentCode() = " + existingResident.getApartmentCode());
            System.out.println("DEBUG: existingResident.getHouseholdCode() = " + existingResident.getHouseholdCode());
            System.out.println("DEBUG: existingResident.getHouseholdId() = " + existingResident.getHouseholdId());
        }
        
        // Lấy apartment code hiện tại của user (nếu có)
        String currentApartmentCode = null;
        if (existingResident != null && existingResident.getApartmentCode() != null) {
            currentApartmentCode = existingResident.getApartmentCode();
        }
        System.out.println("DEBUG: currentApartmentCode = " + currentApartmentCode);
        
        // Find or create household (với validation kiểm tra apartment đã có người ở chưa)
        Integer householdId = findOrCreateHousehold(request, user, userId, currentApartmentCode);
        System.out.println("DEBUG: householdId returned = " + householdId);
        
        // CHỈ CHO PHÉP ĐĂNG KÝ VỚI ROLE "Chủ hộ"
        // Đảm bảo relationship luôn là "Chủ hộ"
        String relationship = "Chủ hộ";
        
        // Create or update resident
        if (existingResident == null) {
            // Create new resident
            Resident resident = new Resident();
            resident.setHouseholdId(householdId);
            resident.setUserId(userId);
            resident.setFullName(request.getFullName());
            resident.setIdCard(request.getIdCard());
            resident.setDateOfBirth(request.getDateOfBirth());
            resident.setGender(request.getGender());
            resident.setRelationship(relationship); // Luôn là "Chủ hộ"
            resident.setPhone(request.getPhone() != null ? request.getPhone() : user.getPhone());
            resident.setEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
            resident.setOccupation(request.getOccupation());
            resident.setPermanentAddress(request.getPermanentAddress());
            resident.setTemporaryAddress(request.getTemporaryAddress());
            resident.setStatus(request.getStatus() != null ? request.getStatus() : "active");
            
            residentRepository.create(resident);
            
            // Tự động tạo fee_collection cho tháng hiện tại (chỉ cho chủ hộ)
            createFeeCollectionForCurrentMonth(householdId);
        } else {
            // Update existing resident
            existingResident.setHouseholdId(householdId);
            existingResident.setFullName(request.getFullName());
            existingResident.setIdCard(request.getIdCard());
            existingResident.setDateOfBirth(request.getDateOfBirth());
            existingResident.setGender(request.getGender());
            existingResident.setRelationship(relationship); // Luôn là "Chủ hộ"
            existingResident.setPhone(request.getPhone() != null ? request.getPhone() : existingResident.getPhone());
            existingResident.setEmail(request.getEmail() != null ? request.getEmail() : existingResident.getEmail());
            existingResident.setOccupation(request.getOccupation());
            existingResident.setPermanentAddress(request.getPermanentAddress());
            existingResident.setTemporaryAddress(request.getTemporaryAddress());
            existingResident.setStatus(request.getStatus() != null ? request.getStatus() : existingResident.getStatus());
            
            residentRepository.update(existingResident);
            
            // Đảm bảo có fee_collection cho tháng hiện tại (chỉ cho chủ hộ)
            createFeeCollectionForCurrentMonth(householdId);
        }
        
        // Update user info if changed
        boolean userUpdated = false;
        if (request.getFullName() != null && !request.getFullName().equals(user.getFullName())) {
            user.setFullName(request.getFullName());
            userUpdated = true;
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
            userUpdated = true;
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            userUpdated = true;
        }
        if (userUpdated) {
            userRepository.update(user);
        }
        
        // Debug: Load lại để kiểm tra
        Resident residentAfterUpdate = residentRepository.findByUserId(userId);
        if (residentAfterUpdate != null) {
            System.out.println("DEBUG: After update - resident.getApartmentCode() = " + residentAfterUpdate.getApartmentCode());
            System.out.println("DEBUG: After update - resident.getHouseholdCode() = " + residentAfterUpdate.getHouseholdCode());
            System.out.println("DEBUG: After update - resident.getHouseholdId() = " + residentAfterUpdate.getHouseholdId());
        }
        System.out.println("=== DEBUG: registerOrUpdatePersonalInfo END ===\n");
    }
    
    /**
     * Get personal information for a user
     */
    public Resident getPersonalInfo(Integer userId) throws DbException {
        System.out.println("=== DEBUG: getPersonalInfo START ===");
        System.out.println("DEBUG: userId = " + userId);
        Resident resident = residentRepository.findByUserId(userId);
        if (resident != null) {
            System.out.println("DEBUG: resident.getApartmentCode() = " + resident.getApartmentCode());
            System.out.println("DEBUG: resident.getHouseholdCode() = " + resident.getHouseholdCode());
            System.out.println("DEBUG: resident.getHouseholdId() = " + resident.getHouseholdId());
        } else {
            System.out.println("DEBUG: resident = NULL");
        }
        System.out.println("=== DEBUG: getPersonalInfo END ===\n");
        return resident;
    }
    
    /**
     * Find or create household based on apartment_code or household_code
     */
    private Integer findOrCreateHousehold(PersonalInfoRequest request, User user, Integer currentUserId, String currentApartmentCode) 
            throws DbException, ValidationException {
        System.out.println("--- DEBUG: findOrCreateHousehold START ---");
        System.out.println("DEBUG: request.getApartmentCode() = " + request.getApartmentCode());
        System.out.println("DEBUG: request.getHouseholdCode() = " + request.getHouseholdCode());
        System.out.println("DEBUG: currentApartmentCode = " + currentApartmentCode);
        
        Integer householdId = null;
        boolean foundByHouseholdCode = false;
        boolean foundByApartmentCode = false;
        
        // Try to find household by household_code
        if (request.getHouseholdCode() != null && !request.getHouseholdCode().trim().isEmpty()) {
            System.out.println("DEBUG: Searching by household_code: " + request.getHouseholdCode().trim());
            householdId = findHouseholdByCode(request.getHouseholdCode());
            if (householdId != null) {
                foundByHouseholdCode = true;
                System.out.println("DEBUG: Found household by household_code, id = " + householdId);
            } else {
                System.out.println("DEBUG: NOT found household by household_code");
            }
        }
        
        // Try to find household by apartment_code
        Integer householdIdByApartment = null;
        if (request.getApartmentCode() != null && !request.getApartmentCode().trim().isEmpty()) {
            // Nếu apartment code mới khác với apartment code hiện tại, kiểm tra xem đã có chủ hộ chưa
            if (currentApartmentCode == null || !currentApartmentCode.equals(request.getApartmentCode().trim())) {
                if (hasChuHoInApartment(request.getApartmentCode(), currentUserId)) {
                    throw new ValidationException("Mã căn hộ này đã có người ở. Vui lòng chọn mã căn hộ khác.");
                }
            }
            System.out.println("DEBUG: Searching by apartment_code: " + request.getApartmentCode().trim());
            householdIdByApartment = findHouseholdByApartmentCode(request.getApartmentCode());
            if (householdIdByApartment != null) {
                foundByApartmentCode = true;
                System.out.println("DEBUG: Found household by apartment_code, id = " + householdIdByApartment);
                String currentCode = getHouseholdCodeById(householdIdByApartment);
                System.out.println("DEBUG: Current household_code of found household = " + currentCode);
            } else {
                System.out.println("DEBUG: NOT found household by apartment_code");
            }
        }
        
        // Nếu tìm thấy theo household_code, sử dụng nó
        // NHƯNG cần kiểm tra và cập nhật apartment_id nếu apartment_code khác
        if (foundByHouseholdCode) {
            System.out.println("DEBUG: Found household by household_code: " + householdId);
            
            // Nếu user đã nhập apartment_code, kiểm tra xem có cần cập nhật apartment_id không
            if (request.getApartmentCode() != null && !request.getApartmentCode().trim().isEmpty()) {
                String requestedApartmentCode = request.getApartmentCode().trim();
                String currentApartmentCodeOfHousehold = getApartmentCodeByHouseholdId(householdId);
                System.out.println("DEBUG: Current apartment_code of household = " + currentApartmentCodeOfHousehold);
                System.out.println("DEBUG: Requested apartment_code = " + requestedApartmentCode);
                
                // Nếu apartment_code khác, cần cập nhật apartment_id
                if (currentApartmentCodeOfHousehold == null || !currentApartmentCodeOfHousehold.equals(requestedApartmentCode)) {
                    System.out.println("DEBUG: Apartment codes differ, updating apartment_id...");
                    // Tìm hoặc tạo apartment với code mới
                    Integer newApartmentId = findOrCreateApartment(request.getApartmentCode().trim());
                    System.out.println("DEBUG: New apartment_id = " + newApartmentId);
                    // Cập nhật apartment_id của household
                    updateHouseholdApartmentId(householdId, newApartmentId);
                    System.out.println("DEBUG: Updated household apartment_id");
                } else {
                    System.out.println("DEBUG: Apartment codes match, no update needed");
                }
            }
            
            System.out.println("DEBUG: Returning household found by household_code: " + householdId);
            System.out.println("--- DEBUG: findOrCreateHousehold END ---");
            return householdId;
        }
        
        // Nếu tìm thấy theo apartment_code
        if (foundByApartmentCode) {
            System.out.println("DEBUG: Found by apartment_code, checking household_code update...");
            // Nếu user đã nhập household_code, cập nhật mã của household đó
            if (request.getHouseholdCode() != null && !request.getHouseholdCode().trim().isEmpty()) {
                String newHouseholdCode = request.getHouseholdCode().trim();
                System.out.println("DEBUG: User provided household_code: " + newHouseholdCode);
                // Kiểm tra xem mã mới có khác với mã hiện tại không
                String currentHouseholdCode = getHouseholdCodeById(householdIdByApartment);
                System.out.println("DEBUG: Current household_code = " + currentHouseholdCode);
                System.out.println("DEBUG: New household_code = " + newHouseholdCode);
                if (currentHouseholdCode == null || !currentHouseholdCode.equals(newHouseholdCode)) {
                    // Chỉ cập nhật nếu mã khác nhau
                    System.out.println("DEBUG: Updating household_code from '" + currentHouseholdCode + "' to '" + newHouseholdCode + "'");
                    updateHouseholdCode(householdIdByApartment, newHouseholdCode);
                } else {
                    System.out.println("DEBUG: household_code unchanged, no update needed");
                }
            } else {
                System.out.println("DEBUG: User did NOT provide household_code");
                // Nếu user không nhập household_code nhưng household hiện tại có mã DEFAULT
                // => Tạo mã mới dựa trên apartment_code
                String currentHouseholdCode = getHouseholdCodeById(householdIdByApartment);
                System.out.println("DEBUG: Current household_code = " + currentHouseholdCode);
                if (currentHouseholdCode != null && currentHouseholdCode.startsWith("DEFAULT-")) {
                    // Tạo mã mới dựa trên apartment_code
                    String newHouseholdCode = request.getApartmentCode().trim() + "-HH";
                    System.out.println("DEBUG: Auto-generating household_code: " + newHouseholdCode);
                    updateHouseholdCode(householdIdByApartment, newHouseholdCode);
                } else {
                    System.out.println("DEBUG: Current household_code is NOT DEFAULT, keeping it");
                }
            }
            System.out.println("DEBUG: Returning household found by apartment_code: " + householdIdByApartment);
            System.out.println("--- DEBUG: findOrCreateHousehold END ---");
            return householdIdByApartment;
        }
        
        // If not found, create a new household
        if (householdId == null) {
            System.out.println("DEBUG: No household found, creating new one...");
            // Kiểm tra lại nếu có apartment code và khác với apartment code hiện tại
            if (request.getApartmentCode() != null && !request.getApartmentCode().trim().isEmpty()) {
                if (currentApartmentCode == null || !currentApartmentCode.equals(request.getApartmentCode().trim())) {
                    if (hasChuHoInApartment(request.getApartmentCode(), currentUserId)) {
                        throw new ValidationException("Mã căn hộ này đã có người ở. Vui lòng chọn mã căn hộ khác.");
                    }
                }
            }
            householdId = createDefaultHousehold(request, user);
            System.out.println("DEBUG: Created new household, id = " + householdId);
        }
        
        System.out.println("--- DEBUG: findOrCreateHousehold END ---");
        return householdId;
    }
    
    /**
     * Kiểm tra xem apartment code đã có chủ hộ chưa (trừ user hiện tại)
     */
    private boolean hasChuHoInApartment(String apartmentCode, Integer excludeUserId) throws DbException {
        String sql = "SELECT COUNT(*) FROM residents r " +
                     "JOIN households h ON r.household_id = h.id " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE a.apartment_code = ? " +
                     "AND r.relationship = 'Chủ hộ' " +
                     "AND r.status = 'active' " +
                     "AND (r.user_id IS NULL OR r.user_id != ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apartmentCode);
            stmt.setInt(2, excludeUserId != null ? excludeUserId : -1);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DbException("Error checking apartment occupancy: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Find household by household_code
     */
    private Integer findHouseholdByCode(String householdCode) throws DbException {
        System.out.println("DEBUG: findHouseholdByCode - searching for: " + householdCode);
        String sql = "SELECT id FROM households WHERE household_code = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, householdCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("DEBUG: findHouseholdByCode - FOUND, id = " + id);
                return id;
            }
            System.out.println("DEBUG: findHouseholdByCode - NOT FOUND");
        } catch (SQLException e) {
            throw new DbException("Error finding household by code: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Find household by apartment_code
     */
    private Integer findHouseholdByApartmentCode(String apartmentCode) throws DbException {
        System.out.println("DEBUG: findHouseholdByApartmentCode - searching for: " + apartmentCode);
        String sql = "SELECT h.id, h.household_code FROM households h " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE a.apartment_code = ? AND h.status = 'active' " +
                     "LIMIT 1";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apartmentCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("household_code");
                System.out.println("DEBUG: findHouseholdByApartmentCode - FOUND, id = " + id + ", household_code = " + code);
                return id;
            }
            System.out.println("DEBUG: findHouseholdByApartmentCode - NOT FOUND");
        } catch (SQLException e) {
            throw new DbException("Error finding household by apartment code: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get household_code by household ID
     */
    private String getHouseholdCodeById(Integer householdId) throws DbException {
        System.out.println("DEBUG: getHouseholdCodeById - householdId = " + householdId);
        String sql = "SELECT household_code FROM households WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String code = rs.getString("household_code");
                System.out.println("DEBUG: getHouseholdCodeById - household_code = " + code);
                return code;
            }
            System.out.println("DEBUG: getHouseholdCodeById - NOT FOUND");
        } catch (SQLException e) {
            throw new DbException("Error getting household code: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Update household_code of an existing household
     */
    private void updateHouseholdCode(Integer householdId, String householdCode) throws DbException {
        System.out.println("DEBUG: updateHouseholdCode - householdId = " + householdId + ", new code = " + householdCode);
        String sql = "UPDATE households SET household_code = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, householdCode);
            stmt.setInt(2, householdId);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG: updateHouseholdCode - rowsAffected = " + rowsAffected);
            if (rowsAffected == 0) {
                throw new DbException("Household not found for update");
            }
            // Verify update
            String verifySql = "SELECT household_code FROM households WHERE id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, householdId);
                ResultSet rs = verifyStmt.executeQuery();
                if (rs.next()) {
                    String updatedCode = rs.getString("household_code");
                    System.out.println("DEBUG: updateHouseholdCode - VERIFIED, updated code = " + updatedCode);
                }
            }
        } catch (SQLException e) {
            System.out.println("DEBUG: updateHouseholdCode - ERROR: " + e.getMessage());
            // Nếu lỗi do duplicate household_code, không cập nhật
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("household_code"))) {
                throw new DbException("Mã hộ khẩu đã tồn tại. Vui lòng chọn mã khác.");
            }
            throw new DbException("Error updating household code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a default household for user
     * Sử dụng household_code từ request nếu có, nếu không thì tạo mã mặc định
     */
    private Integer createDefaultHousehold(PersonalInfoRequest request, User user) throws DbException {
        // Create a default apartment if needed
        Integer apartmentId = createDefaultApartment(request);
        
        // Sử dụng household_code từ request nếu có, nếu không thì tạo mã mặc định
        String householdCode;
        if (request.getHouseholdCode() != null && !request.getHouseholdCode().trim().isEmpty()) {
            householdCode = request.getHouseholdCode().trim();
            
            // Kiểm tra xem household_code đã tồn tại chưa
            Integer existingHouseholdId = findHouseholdByCode(householdCode);
            if (existingHouseholdId != null) {
                // Nếu đã tồn tại, trả về household_id đó
                return existingHouseholdId;
            }
        } else {
            // Tạo mã mặc định nếu user không nhập
            householdCode = "USER-" + user.getId() + "-" + System.currentTimeMillis();
        }
        
        String sql = "INSERT INTO households (apartment_id, household_code, owner_name, owner_phone, owner_email, " +
                     "registration_date, status) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, apartmentId);
            stmt.setString(2, householdCode);
            stmt.setString(3, request.getFullName() != null ? request.getFullName() : user.getFullName());
            stmt.setString(4, request.getPhone() != null ? request.getPhone() : user.getPhone());
            stmt.setString(5, request.getEmail() != null ? request.getEmail() : user.getEmail());
            stmt.setDate(6, Date.valueOf(LocalDate.now()));
            stmt.setString(7, "active");
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new DbException("Failed to create household");
        } catch (SQLException e) {
            // Nếu lỗi do duplicate household_code, thử tìm lại
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("household_code"))) {
                Integer existingHouseholdId = findHouseholdByCode(householdCode);
                if (existingHouseholdId != null) {
                    return existingHouseholdId;
                }
            }
            throw new DbException("Error creating household: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get apartment_code by household ID
     */
    private String getApartmentCodeByHouseholdId(Integer householdId) throws DbException {
        System.out.println("DEBUG: getApartmentCodeByHouseholdId - householdId = " + householdId);
        String sql = "SELECT a.apartment_code FROM households h " +
                     "JOIN apartments a ON h.apartment_id = a.id " +
                     "WHERE h.id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, householdId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String code = rs.getString("apartment_code");
                System.out.println("DEBUG: getApartmentCodeByHouseholdId - apartment_code = " + code);
                return code;
            }
            System.out.println("DEBUG: getApartmentCodeByHouseholdId - NOT FOUND");
        } catch (SQLException e) {
            throw new DbException("Error getting apartment code by household id: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Find or create apartment by apartment_code
     */
    private Integer findOrCreateApartment(String apartmentCode) throws DbException {
        System.out.println("DEBUG: findOrCreateApartment - apartmentCode = " + apartmentCode);
        // Try to find existing apartment
        String sql = "SELECT id FROM apartments WHERE apartment_code = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apartmentCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("DEBUG: findOrCreateApartment - FOUND, id = " + id);
                return id;
            }
        } catch (SQLException e) {
            throw new DbException("Error finding apartment: " + e.getMessage(), e);
        }
        
        // Not found, create new apartment
        System.out.println("DEBUG: findOrCreateApartment - NOT FOUND, creating new...");
        String insertSql = "INSERT INTO apartments (building_number, floor_number, room_number, apartment_code, area, number_of_rooms, status) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, "A");
            stmt.setInt(2, 1);
            stmt.setString(3, "01");
            stmt.setString(4, apartmentCode);
            stmt.setBigDecimal(5, new java.math.BigDecimal("60.00"));
            stmt.setInt(6, 2);
            stmt.setString(7, "occupied");
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("DEBUG: findOrCreateApartment - CREATED, id = " + id);
                return id;
            }
            throw new DbException("Failed to create apartment");
        } catch (SQLException e) {
            // Nếu lỗi do duplicate apartment_code, thử tìm lại
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("apartment_code"))) {
                try (Connection conn2 = JdbcUtils.getConnection();
                     PreparedStatement stmt2 = conn2.prepareStatement(sql)) {
                    stmt2.setString(1, apartmentCode);
                    ResultSet rs2 = stmt2.executeQuery();
                    if (rs2.next()) {
                        int id = rs2.getInt("id");
                        System.out.println("DEBUG: findOrCreateApartment - FOUND after duplicate error, id = " + id);
                        return id;
                    }
                } catch (SQLException e2) {
                    throw new DbException("Error finding apartment after duplicate: " + e2.getMessage(), e2);
                }
            }
            throw new DbException("Error creating apartment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update apartment_id of an existing household
     */
    private void updateHouseholdApartmentId(Integer householdId, Integer apartmentId) throws DbException {
        System.out.println("DEBUG: updateHouseholdApartmentId - householdId = " + householdId + ", new apartmentId = " + apartmentId);
        String sql = "UPDATE households SET apartment_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, apartmentId);
            stmt.setInt(2, householdId);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG: updateHouseholdApartmentId - rowsAffected = " + rowsAffected);
            if (rowsAffected == 0) {
                throw new DbException("Household not found for update");
            }
            // Verify update
            String verifySql = "SELECT a.apartment_code FROM households h " +
                              "JOIN apartments a ON h.apartment_id = a.id " +
                              "WHERE h.id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, householdId);
                ResultSet rs = verifyStmt.executeQuery();
                if (rs.next()) {
                    String updatedCode = rs.getString("apartment_code");
                    System.out.println("DEBUG: updateHouseholdApartmentId - VERIFIED, updated apartment_code = " + updatedCode);
                }
            }
        } catch (SQLException e) {
            System.out.println("DEBUG: updateHouseholdApartmentId - ERROR: " + e.getMessage());
            throw new DbException("Error updating household apartment_id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a default apartment if needed
     * Sử dụng apartment_code từ request nếu có, nếu không thì tạo mã mặc định
     */
    private Integer createDefaultApartment(PersonalInfoRequest request) throws DbException {
        String apartmentCode;
        
        // Try to find existing apartment first
        if (request.getApartmentCode() != null && !request.getApartmentCode().trim().isEmpty()) {
            String sql = "SELECT id FROM apartments WHERE apartment_code = ?";
            try (Connection conn = JdbcUtils.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, request.getApartmentCode().trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                throw new DbException("Error finding apartment: " + e.getMessage(), e);
            }
            // Nếu không tìm thấy, sử dụng mã mà user nhập để tạo mới
            apartmentCode = request.getApartmentCode().trim();
        } else {
            // Nếu user không nhập apartment_code, tạo mã mặc định
            apartmentCode = "DEFAULT-" + System.currentTimeMillis();
        }
        
        // Create apartment với mã đã xác định
        String sql = "INSERT INTO apartments (building_number, floor_number, room_number, apartment_code, area, number_of_rooms, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "A");
            stmt.setInt(2, 1);
            stmt.setString(3, "01");
            stmt.setString(4, apartmentCode);
            stmt.setBigDecimal(5, new java.math.BigDecimal("60.00"));
            stmt.setInt(6, 2);
            stmt.setString(7, "occupied");
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new DbException("Failed to create apartment");
        } catch (SQLException e) {
            // Nếu lỗi do duplicate apartment_code, thử tìm lại
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || 
                e.getMessage().contains("unique constraint") || 
                e.getMessage().contains("apartment_code"))) {
                String findSql = "SELECT id FROM apartments WHERE apartment_code = ?";
                try (Connection conn2 = JdbcUtils.getConnection();
                     PreparedStatement stmt2 = conn2.prepareStatement(findSql)) {
                    stmt2.setString(1, apartmentCode);
                    ResultSet rs2 = stmt2.executeQuery();
                    if (rs2.next()) {
                        return rs2.getInt("id");
                    }
                } catch (SQLException e2) {
                    throw new DbException("Error finding apartment after duplicate: " + e2.getMessage(), e2);
                }
            }
            throw new DbException("Error creating apartment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo fee_collection cho tháng hiện tại nếu chưa có
     * Số tiền mặc định: 500,000 VNĐ (có thể cấu hình sau)
     * Kiểm tra trực tiếp trong database để đảm bảo chính xác
     */
    private void createFeeCollectionForCurrentMonth(Integer householdId) throws DbException {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Kiểm tra trực tiếp trong database xem đã có fee_collection cho tháng này chưa
        // Không dùng findByHouseholdId vì nó có filter phức tạp
        String checkSql = "SELECT COUNT(*) FROM fee_collections " +
                          "WHERE household_id = ? AND month = ? AND year = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, householdId);
            stmt.setInt(2, currentMonth);
            stmt.setInt(3, currentYear);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // Đã có fee_collection cho tháng này, không cần tạo mới
                return;
            }
        } catch (SQLException e) {
            throw new DbException("Error checking existing fee collection: " + e.getMessage(), e);
        }
        
        // Tạo fee_collection mới với số tiền mặc định
        // Số tiền có thể thay đổi tùy theo yêu cầu (hiện tại: 500,000 VNĐ)
        FeeCollection fee = new FeeCollection();
        fee.setHouseholdId(householdId);
        fee.setMonth(currentMonth);
        fee.setYear(currentYear);
        fee.setAmount(new BigDecimal("500000")); // Số tiền mặc định
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setStatus("unpaid");
        feeCollectionRepository.create(fee);
    }
}

