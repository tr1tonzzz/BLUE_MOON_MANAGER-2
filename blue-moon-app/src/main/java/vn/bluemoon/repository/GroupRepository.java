package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Group;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Group entity
 */
public class GroupRepository {
    
    /**
     * Find all groups
     */
    public List<Group> findAll() throws DbException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups ORDER BY name";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all groups: " + e.getMessage(), e);
        }
        return groups;
    }

    /**
     * Find group by ID
     */
    public Group findById(Integer id) throws DbException {
        String sql = "SELECT * FROM groups WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGroup(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding group by id: " + e.getMessage(), e);
        }
    }

    /**
     * Find groups by user ID
     */
    public List<Group> findByUserId(Integer userId) throws DbException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.* FROM groups g " +
                     "INNER JOIN user_roles ur ON g.id = ur.group_id " +
                     "WHERE ur.user_id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding groups by user id: " + e.getMessage(), e);
        }
        return groups;
    }

    /**
     * Create group
     */
    public Group create(Group group) throws DbException {
        String sql = "INSERT INTO groups (name, description) VALUES (?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DbException("Creating group failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    group.setId(generatedKeys.getInt(1));
                } else {
                    throw new DbException("Creating group failed, no ID obtained.");
                }
            }
            return group;
        } catch (SQLException e) {
            throw new DbException("Error creating group: " + e.getMessage(), e);
        }
    }

    /**
     * Update group
     */
    public void update(Group group) throws DbException {
        String sql = "UPDATE groups SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setInt(3, group.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating group: " + e.getMessage(), e);
        }
    }

    /**
     * Delete group
     */
    public void delete(Integer id) throws DbException {
        String sql = "DELETE FROM groups WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting group: " + e.getMessage(), e);
        }
    }

    private Group mapResultSetToGroup(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            group.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            group.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return group;
    }
}


