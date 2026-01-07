package vn.bluemoon.util;

import vn.bluemoon.config.DbConfig;
import vn.bluemoon.exception.DbException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for JDBC operations
 */
public class JdbcUtils {
    private static final DbConfig dbConfig = DbConfig.getInstance();

    /**
     * Get database connection
     * @return Connection object
     * @throws DbException if connection fails
     */
    public static Connection getConnection() throws DbException {
        try {
            Class.forName(dbConfig.getDriver());
            return DriverManager.getConnection(
                dbConfig.getUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword()
            );
        } catch (ClassNotFoundException e) {
            throw new DbException("Database driver not found: " + dbConfig.getDriver(), e);
        } catch (SQLException e) {
            String errorMsg = "Failed to connect to database: " + e.getMessage();
            // Add helpful message for common errors
            String driver = dbConfig.getDriver();
            boolean isPostgreSQL = driver.contains("postgresql");
            String dbType = isPostgreSQL ? "PostgreSQL" : "MySQL";
            String defaultPort = isPostgreSQL ? "5432" : "3306";
            
            if (e.getMessage().contains("Communications link failure") || 
                e.getMessage().contains("Connection refused")) {
                errorMsg += "\n\nVui lòng kiểm tra:\n" +
                           "1. " + dbType + " service đang chạy\n" +
                           "2. Port " + defaultPort + " không bị chặn\n" +
                           "3. Xem file HUONG-DAN-" + (isPostgreSQL ? "POSTGRESQL" : "THIET-LAP-DATABASE") + ".md để biết chi tiết";
            } else if (e.getMessage().contains("Unknown database") || 
                       e.getMessage().contains("does not exist")) {
                errorMsg += "\n\nDatabase chưa được tạo. Ứng dụng sẽ tự động tạo database khi khởi động.";
            }
            throw new DbException(errorMsg, e);
        }
    }

    /**
     * Close connection safely
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                AppLogger.error("Error closing connection", e);
            }
        }
    }
}
