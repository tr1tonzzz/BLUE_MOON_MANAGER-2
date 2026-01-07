package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for User entity
 */
public class UserRepository {
    
    /**
     * Find user by username
     */
    public User findByUsername(String username) throws DbException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding user by username: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) throws DbException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding user by email: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by ID
     */
    public User findById(Integer id) throws DbException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding user by id: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by Facebook ID
     */
    public User findByFacebookId(String facebookId) throws DbException {
        String sql = "SELECT * FROM users WHERE facebook_id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, facebookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding user by Facebook ID: " + e.getMessage(), e);
        }
    }

    /**
     * Create new user
     */
    public User create(User user) throws DbException {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, phone, address, " +
                     "is_active, must_change_password, password_change_required_date, password_change_period_days, " +
                     "last_password_change_date, facebook_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getAddress());
            stmt.setBoolean(7, user.getIsActive() != null ? user.getIsActive() : true);
            stmt.setBoolean(8, user.getMustChangePassword() != null ? user.getMustChangePassword() : false);
            if (user.getPasswordChangeRequiredDate() != null) {
                stmt.setDate(9, java.sql.Date.valueOf(user.getPasswordChangeRequiredDate()));
            } else {
                stmt.setNull(9, java.sql.Types.DATE);
            }
            if (user.getPasswordChangePeriodDays() != null) {
                stmt.setInt(10, user.getPasswordChangePeriodDays());
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }
            if (user.getLastPasswordChangeDate() != null) {
                stmt.setDate(11, java.sql.Date.valueOf(user.getLastPasswordChangeDate()));
            } else {
                stmt.setNull(11, java.sql.Types.DATE);
            }
            stmt.setString(12, user.getFacebookId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DbException("Creating user failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new DbException("Creating user failed, no ID obtained.");
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DbException("Error creating user: " + e.getMessage(), e);
        }
    }

    /**
     * Update user
     */
    public void update(User user) throws DbException {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, full_name = ?, " +
                     "phone = ?, address = ?, is_active = ?, must_change_password = ?, " +
                     "password_change_required_date = ?, password_change_period_days = ?, " +
                     "last_password_change_date = ?, facebook_id = ? WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getAddress());
            stmt.setBoolean(7, user.getIsActive() != null ? user.getIsActive() : true);
            stmt.setBoolean(8, user.getMustChangePassword() != null ? user.getMustChangePassword() : false);
            if (user.getPasswordChangeRequiredDate() != null) {
                stmt.setDate(9, java.sql.Date.valueOf(user.getPasswordChangeRequiredDate()));
            } else {
                stmt.setNull(9, java.sql.Types.DATE);
            }
            if (user.getPasswordChangePeriodDays() != null) {
                stmt.setInt(10, user.getPasswordChangePeriodDays());
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }
            if (user.getLastPasswordChangeDate() != null) {
                stmt.setDate(11, java.sql.Date.valueOf(user.getLastPasswordChangeDate()));
            } else {
                stmt.setNull(11, java.sql.Types.DATE);
            }
            stmt.setString(12, user.getFacebookId());
            stmt.setInt(13, user.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating user: " + e.getMessage(), e);
        }
    }

    /**
     * Search users
     */
    public List<User> search(String username, String email, String fullName, String phone) throws DbException {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (username != null && !username.trim().isEmpty()) {
            sql.append(" AND username LIKE ?");
            params.add("%" + username + "%");
        }
        if (email != null && !email.trim().isEmpty()) {
            sql.append(" AND email LIKE ?");
            params.add("%" + email + "%");
        }
        if (fullName != null && !fullName.trim().isEmpty()) {
            sql.append(" AND full_name LIKE ?");
            params.add("%" + fullName + "%");
        }
        if (phone != null && !phone.trim().isEmpty()) {
            sql.append(" AND phone LIKE ?");
            params.add("%" + phone + "%");
        }
        
        sql.append(" ORDER BY created_at DESC");
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error searching users: " + e.getMessage(), e);
        }
        return users;
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) throws DbException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking username existence: " + e.getMessage(), e);
        }
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) throws DbException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking email existence: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete user by ID
     */
    public void delete(Integer userId) throws DbException {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting user: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        
        // Kiểm tra và đọc các cột mới (có thể chưa tồn tại trong database cũ)
        try {
            java.sql.Date passwordChangeRequiredDate = rs.getDate("password_change_required_date");
            if (passwordChangeRequiredDate != null) {
                user.setPasswordChangeRequiredDate(passwordChangeRequiredDate.toLocalDate());
            }
        } catch (SQLException e) {
            // Cột chưa tồn tại, bỏ qua
        }
        
        try {
            Integer passwordChangePeriodDays = rs.getInt("password_change_period_days");
            if (!rs.wasNull()) {
                user.setPasswordChangePeriodDays(passwordChangePeriodDays);
            }
        } catch (SQLException e) {
            // Cột chưa tồn tại, bỏ qua
        }
        
        try {
            java.sql.Date lastPasswordChangeDate = rs.getDate("last_password_change_date");
            if (lastPasswordChangeDate != null) {
                user.setLastPasswordChangeDate(lastPasswordChangeDate.toLocalDate());
            }
        } catch (SQLException e) {
            // Cột chưa tồn tại, bỏ qua
        }
        
        user.setFacebookId(rs.getString("facebook_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    }
}


