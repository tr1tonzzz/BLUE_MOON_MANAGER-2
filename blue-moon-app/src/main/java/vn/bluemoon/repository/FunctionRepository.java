package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Function;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Function entity
 */
public class FunctionRepository {
    
    /**
     * Find all functions with function group name
     */
    public List<Function> findAll() throws DbException {
        List<Function> functions = new ArrayList<>();
        String sql = "SELECT f.*, fg.name as function_group_name " +
                     "FROM functions f " +
                     "INNER JOIN function_groups fg ON f.function_group_id = fg.id " +
                     "ORDER BY fg.name, f.name";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                functions.add(mapResultSetToFunction(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all functions: " + e.getMessage(), e);
        }
        return functions;
    }

    /**
     * Find function by ID
     */
    public Function findById(Integer id) throws DbException {
        String sql = "SELECT f.*, fg.name as function_group_name " +
                     "FROM functions f " +
                     "INNER JOIN function_groups fg ON f.function_group_id = fg.id " +
                     "WHERE f.id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFunction(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding function by id: " + e.getMessage(), e);
        }
    }

    /**
     * Find functions by group ID
     */
    public List<Function> findByGroupId(Integer groupId) throws DbException {
        List<Function> functions = new ArrayList<>();
        String sql = "SELECT f.*, fg.name as function_group_name " +
                     "FROM functions f " +
                     "INNER JOIN function_groups fg ON f.function_group_id = fg.id " +
                     "INNER JOIN group_functions gf ON f.id = gf.function_id " +
                     "WHERE gf.group_id = ? " +
                     "ORDER BY fg.name, f.name";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                functions.add(mapResultSetToFunction(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding functions by group id: " + e.getMessage(), e);
        }
        return functions;
    }

    /**
     * Create function
     */
    public Function create(Function function) throws DbException {
        String sql = "INSERT INTO functions (name, function_group_id, boundary_class, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, function.getName());
            stmt.setInt(2, function.getFunctionGroupId());
            stmt.setString(3, function.getBoundaryClass());
            stmt.setString(4, function.getDescription());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DbException("Creating function failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    function.setId(generatedKeys.getInt(1));
                } else {
                    throw new DbException("Creating function failed, no ID obtained.");
                }
            }
            return function;
        } catch (SQLException e) {
            throw new DbException("Error creating function: " + e.getMessage(), e);
        }
    }

    /**
     * Update function
     */
    public void update(Function function) throws DbException {
        String sql = "UPDATE functions SET name = ?, function_group_id = ?, boundary_class = ?, description = ? WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, function.getName());
            stmt.setInt(2, function.getFunctionGroupId());
            stmt.setString(3, function.getBoundaryClass());
            stmt.setString(4, function.getDescription());
            stmt.setInt(5, function.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error updating function: " + e.getMessage(), e);
        }
    }

    /**
     * Delete function
     */
    public void delete(Integer id) throws DbException {
        String sql = "DELETE FROM functions WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error deleting function: " + e.getMessage(), e);
        }
    }

    /**
     * Check if name exists
     */
    public boolean existsByName(String name, Integer excludeId) throws DbException {
        String sql = "SELECT COUNT(*) FROM functions WHERE name = ?";
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking function name existence: " + e.getMessage(), e);
        }
    }

    /**
     * Check if boundary class exists
     */
    public boolean existsByBoundaryClass(String boundaryClass, Integer excludeId) throws DbException {
        String sql = "SELECT COUNT(*) FROM functions WHERE boundary_class = ?";
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, boundaryClass);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new DbException("Error checking boundary class existence: " + e.getMessage(), e);
        }
    }

    private Function mapResultSetToFunction(ResultSet rs) throws SQLException {
        Function function = new Function();
        function.setId(rs.getInt("id"));
        function.setName(rs.getString("name"));
        function.setFunctionGroupId(rs.getInt("function_group_id"));
        function.setBoundaryClass(rs.getString("boundary_class"));
        function.setDescription(rs.getString("description"));
        function.setFunctionGroupName(rs.getString("function_group_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            function.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            function.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return function;
    }
}


