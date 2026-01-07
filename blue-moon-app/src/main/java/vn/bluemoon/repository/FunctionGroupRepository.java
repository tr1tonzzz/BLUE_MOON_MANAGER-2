package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FunctionGroup;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for FunctionGroup entity
 */
public class FunctionGroupRepository {
    
    /**
     * Find all function groups
     */
    public List<FunctionGroup> findAll() throws DbException {
        List<FunctionGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM function_groups ORDER BY name";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapResultSetToFunctionGroup(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all function groups: " + e.getMessage(), e);
        }
        return groups;
    }

    /**
     * Find function group by ID
     */
    public FunctionGroup findById(Integer id) throws DbException {
        String sql = "SELECT * FROM function_groups WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFunctionGroup(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding function group by id: " + e.getMessage(), e);
        }
    }

    private FunctionGroup mapResultSetToFunctionGroup(ResultSet rs) throws SQLException {
        FunctionGroup group = new FunctionGroup();
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


