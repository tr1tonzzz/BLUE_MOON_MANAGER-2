package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Role;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Role (user-group relationship)
 */
public class RoleRepository {
    
    /**
     * Assign role to user
     */
    public void assignRoleToUser(Integer userId, Integer groupId) throws DbException {
        String sql = "INSERT INTO user_roles (user_id, group_id) VALUES (?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error assigning role to user: " + e.getMessage(), e);
        }
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(Integer userId, Integer groupId) throws DbException {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND group_id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error removing role from user: " + e.getMessage(), e);
        }
    }

    /**
     * Get all roles for a user
     */
    public List<Role> findByUserId(Integer userId) throws DbException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM user_roles WHERE user_id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding roles by user id: " + e.getMessage(), e);
        }
        return roles;
    }

    /**
     * Check if user has role
     */
    public boolean userHasRole(Integer userId, Integer groupId) throws DbException {
        String sql = "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND group_id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking user role: " + e.getMessage(), e);
        }
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setUserId(rs.getInt("user_id"));
        role.setGroupId(rs.getInt("group_id"));
        Timestamp assignedAt = rs.getTimestamp("assigned_at");
        if (assignedAt != null) {
            role.setAssignedAt(assignedAt.toLocalDateTime());
        }
        return role;
    }
}

















