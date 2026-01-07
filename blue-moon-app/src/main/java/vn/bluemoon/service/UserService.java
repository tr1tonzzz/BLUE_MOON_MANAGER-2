package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.RegisterRequest;
import vn.bluemoon.model.dto.UserSearchRequest;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.repository.UserRepository;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.repository.FeeCollectionRepository;
import vn.bluemoon.util.PasswordHasher;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.util.List;

/**
 * User service
 */
public class UserService {
    private final UserRepository userRepository = new UserRepository();
    private final ResidentRepository residentRepository = new ResidentRepository();
    private final FeeCollectionRepository feeCollectionRepository = new FeeCollectionRepository();

    /**
     * Register new user
     * @param request Registration request
     * @return Created user
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public User register(RegisterRequest request) throws ValidationException, DbException {
        // Validate input
        Validators.validateUsername(request.getUsername());
        Validators.validateEmail(request.getEmail());
        Validators.validateRequired(request.getFullName(), "Họ và tên");
        Validators.validatePassword(request.getPassword());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Tên đăng nhập đã tồn tại");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email đã tồn tại");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(PasswordHasher.hash(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setIsActive(true);
        user.setMustChangePassword(false);

        return userRepository.create(user);
    }

    /**
     * Search users
     * @param request Search request
     * @return List of users
     * @throws DbException if database error occurs
     */
    public List<User> searchUsers(UserSearchRequest request) throws DbException {
        return userRepository.search(
            request.getUsername(),
            request.getEmail(),
            request.getFullName(),
            request.getPhone()
        );
    }

    /**
     * Update user
     * @param user User to update
     * @throws DbException if database error occurs
     */
    public void updateUser(User user) throws DbException {
        userRepository.update(user);
    }

    /**
     * Disable user account
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void disableUser(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setIsActive(false);
            userRepository.update(user);
        }
    }

    /**
     * Enable user account
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void enableUser(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setIsActive(true);
            userRepository.update(user);
        }
    }

    /**
     * Require password change for user
     * @param userId User ID
     * @throws DbException if database error occurs
     */
    public void requirePasswordChange(Integer userId) throws DbException {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setMustChangePassword(true);
            userRepository.update(user);
        }
    }

    /**
     * Change user password
     * @param userId User ID
     * @param newPassword New password
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public void changePassword(Integer userId, String newPassword) throws ValidationException, DbException {
        Validators.validatePassword(newPassword);
        
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setPasswordHash(PasswordHasher.hash(newPassword));
            user.setMustChangePassword(false);
            // Cập nhật ngày đổi mật khẩu lần cuối
            user.setLastPasswordChangeDate(java.time.LocalDate.now());
            // Hủy yêu cầu đổi mật khẩu ngay lập tức nếu đã đổi
            if (user.getPasswordChangeRequiredDate() != null) {
                java.time.LocalDate today = java.time.LocalDate.now();
                if (today.isAfter(user.getPasswordChangeRequiredDate()) || 
                    today.isEqual(user.getPasswordChangeRequiredDate())) {
                    user.setPasswordChangeRequiredDate(null);
                }
            }
            userRepository.update(user);
        }
    }
    
    /**
     * Delete user and related data
     * - Find resident linked to this user
     * - If resident is "Chủ hộ", delete all fee collections for the household
     * - Delete the resident
     * - If household has no other residents, delete the household
     * - Delete the user
     */
    public void deleteUser(Integer userId) throws DbException {
        // Find resident linked to this user
        Resident resident = residentRepository.findByUserId(userId);
        
        if (resident != null) {
            Integer householdId = resident.getHouseholdId();
            Integer residentId = resident.getId();
            boolean isChuHo = "Chủ hộ".equals(resident.getRelationship());
            
            // If resident is "Chủ hộ", delete all fee collections for the household
            if (isChuHo) {
                feeCollectionRepository.deleteByHouseholdId(householdId);
            }
            
            // Check if household has any other residents BEFORE deleting
            boolean hasOtherResidents = residentRepository.hasOtherResidents(householdId, residentId);
            
            // Delete the resident
            residentRepository.delete(residentId);
            
            // If no other residents, delete the household
            if (!hasOtherResidents) {
                deleteHousehold(householdId);
            }
        }
        
        // Delete the user
        userRepository.delete(userId);
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
}






