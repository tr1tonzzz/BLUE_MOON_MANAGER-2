package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.util.JdbcUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repository for Apartment entity
 */
public class ApartmentRepository {
    
    /**
     * Count all apartments
     */
    public int countAll() throws DbException {
        String sql = "SELECT COUNT(*) as count FROM apartments";
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            throw new DbException("Error counting apartments: " + e.getMessage(), e);
        }
        return 0;
    }
}





