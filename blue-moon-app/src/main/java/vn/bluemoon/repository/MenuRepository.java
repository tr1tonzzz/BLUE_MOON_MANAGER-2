package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Menu;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Menu entity
 */
public class MenuRepository {
    
    /**
     * Find all menus
     */
    public List<Menu> findAll() throws DbException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menus ORDER BY display_order, name";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                menus.add(mapResultSetToMenu(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding all menus: " + e.getMessage(), e);
        }
        return menus;
    }

    /**
     * Find menus by parent ID
     */
    public List<Menu> findByParentId(Integer parentId) throws DbException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menus WHERE parent_id = ? ORDER BY display_order, name";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, parentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                menus.add(mapResultSetToMenu(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding menus by parent id: " + e.getMessage(), e);
        }
        return menus;
    }

    /**
     * Find root menus (parent_id is NULL)
     */
    public List<Menu> findRootMenus() throws DbException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menus WHERE parent_id IS NULL ORDER BY display_order, name";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                menus.add(mapResultSetToMenu(rs));
            }
        } catch (SQLException e) {
            throw new DbException("Error finding root menus: " + e.getMessage(), e);
        }
        return menus;
    }

    /**
     * Create menu
     */
    public Menu create(Menu menu) throws DbException {
        String sql = "INSERT INTO menus (name, parent_id, function_id, display_order, icon) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, menu.getName());
            if (menu.getParentId() != null) {
                stmt.setInt(2, menu.getParentId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            if (menu.getFunctionId() != null) {
                stmt.setInt(3, menu.getFunctionId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setInt(4, menu.getDisplayOrder() != null ? menu.getDisplayOrder() : 0);
            stmt.setString(5, menu.getIcon());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DbException("Creating menu failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    menu.setId(generatedKeys.getInt(1));
                } else {
                    throw new DbException("Creating menu failed, no ID obtained.");
                }
            }
            return menu;
        } catch (SQLException e) {
            throw new DbException("Error creating menu: " + e.getMessage(), e);
        }
    }

    /**
     * Find menu by ID
     */
    public Menu findById(Integer id) throws DbException {
        String sql = "SELECT * FROM menus WHERE id = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToMenu(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding menu by id: " + e.getMessage(), e);
        }
    }

    private Menu mapResultSetToMenu(ResultSet rs) throws SQLException {
        Menu menu = new Menu();
        menu.setId(rs.getInt("id"));
        menu.setName(rs.getString("name"));
        Integer parentId = rs.getObject("parent_id", Integer.class);
        menu.setParentId(parentId);
        Integer functionId = rs.getObject("function_id", Integer.class);
        menu.setFunctionId(functionId);
        menu.setDisplayOrder(rs.getInt("display_order"));
        menu.setIcon(rs.getString("icon"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            menu.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            menu.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return menu;
    }
}


