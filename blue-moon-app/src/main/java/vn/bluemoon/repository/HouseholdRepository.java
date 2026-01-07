package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.util.JdbcUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repository for Household entity
 */
public class HouseholdRepository {
    
    /**
     * Count all households that have at least one resident with relationship = 'Chủ hộ'
     * This ensures that households without residents are not counted
     */
    public int countAll() throws DbException {
        boolean isPostgreSQL = isPostgreSQL();
        String sql;
        
        if (isPostgreSQL) {
            // PostgreSQL: Count households that have at least one resident with relationship = 'Chủ hộ'
            sql = "SELECT COUNT(DISTINCT h.id) as count " +
                  "FROM households h " +
                  "JOIN apartments a ON h.apartment_id = a.id " +
                  "INNER JOIN residents r ON r.household_id = h.id " +
                  "AND r.relationship = 'Chủ hộ' " +
                  "WHERE a.apartment_code NOT LIKE 'DEFAULT-%'";
        } else {
            // MySQL: Count households that have at least one resident with relationship = 'Chủ hộ'
            sql = "SELECT COUNT(DISTINCT h.id) as count " +
                  "FROM households h " +
                  "JOIN apartments a ON h.apartment_id = a.id " +
                  "INNER JOIN residents r ON r.household_id = h.id " +
                  "AND r.relationship = 'Chủ hộ' " +
                  "WHERE a.apartment_code NOT LIKE 'DEFAULT-%'";
        }
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            throw new DbException("Error counting households: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Check if database is PostgreSQL
     */
    private boolean isPostgreSQL() {
        try {
            String driver = vn.bluemoon.config.DbConfig.getInstance().getDriver();
            return driver != null && driver.contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }
}



