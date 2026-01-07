package vn.bluemoon.util;

import vn.bluemoon.config.DbConfig;
import vn.bluemoon.exception.DbException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility class to initialize database
 */
public class DatabaseInitializer {
    private static final DbConfig dbConfig = DbConfig.getInstance();
    private static boolean initialized = false;

    /**
     * Initialize database - create if not exists and run schema
     * @throws DbException if initialization fails
     */
    public static void initialize() throws DbException {
        if (initialized) {
            return;
        }

        try {
            // First, try to connect to the database
            testConnection();
            // Remove old unique constraint to allow multiple fees per month/year
            removeOldUniqueConstraint();
            initialized = true;
            AppLogger.info("Database connection successful");
        } catch (DbException e) {
            // If connection fails, try to create database
            AppLogger.warn("Database connection failed, attempting to create database...");
            
            if (createDatabaseIfNotExists()) {
                try {
                    // Try to create tables
                    createTablesIfNotExists();
                    initialized = true;
                    AppLogger.info("Database initialized successfully");
                } catch (Exception ex) {
                    AppLogger.error("Failed to create tables", ex);
                    String dbType = dbConfig.getDriver().contains("postgresql") ? "PostgreSQL" : "MySQL";
                    throw new DbException(
                        "Không thể tạo database. Vui lòng:\n" +
                        "1. Đảm bảo " + dbType + " đang chạy\n" +
                        "2. Chạy file schema" + (dbType.equals("PostgreSQL") ? "-postgresql" : "") + ".sql trong database client\n" +
                        "3. Xem file HUONG-DAN-THIET-LAP-DATABASE.md để biết chi tiết",
                        ex
                    );
                }
            } else {
                String dbType = dbConfig.getDriver().contains("postgresql") ? "PostgreSQL" : "MySQL";
                throw new DbException(
                    "Không thể kết nối đến " + dbType + ". Vui lòng:\n" +
                    "1. Đảm bảo " + dbType + " service đang chạy\n" +
                    "2. Kiểm tra username và password trong application.properties\n" +
                    "3. Xem file HUONG-DAN-THIET-LAP-DATABASE.md để biết chi tiết\n\n" +
                    "Lỗi: " + e.getMessage()
                );
            }
        }
    }

    /**
     * Test database connection
     */
    private static void testConnection() throws DbException {
        try {
            Class.forName(dbConfig.getDriver());
            String url = dbConfig.getUrl();
            Connection conn = DriverManager.getConnection(
                url,
                dbConfig.getUsername(),
                dbConfig.getPassword()
            );
            conn.close();
        } catch (ClassNotFoundException e) {
            throw new DbException("Database driver not found: " + dbConfig.getDriver(), e);
        } catch (SQLException e) {
            throw new DbException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Create database if it doesn't exist
     */
    private static boolean createDatabaseIfNotExists() {
        try {
            String baseUrl = dbConfig.getUrl();
            String driver = dbConfig.getDriver();
            boolean isPostgreSQL = driver.contains("postgresql");
            
            // Extract base URL (remove database name)
            int lastSlash = baseUrl.lastIndexOf('/');
            if (lastSlash == -1) {
                // No slash found, use default server URL
                if (isPostgreSQL) {
                    baseUrl = "jdbc:postgresql://localhost:5432/blue_moon";
                } else {
                    baseUrl = "jdbc:mysql://localhost:3306/blue_moon";
                }
                lastSlash = baseUrl.lastIndexOf('/');
            }
            
            String serverUrl = baseUrl.substring(0, lastSlash);
            if (serverUrl.contains("?")) {
                serverUrl = serverUrl.substring(0, serverUrl.indexOf('?'));
            }
            
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(
                serverUrl,
                dbConfig.getUsername(),
                dbConfig.getPassword()
            );
            
            Statement stmt = conn.createStatement();
            String dbName = extractDatabaseName(baseUrl);
            
            if (isPostgreSQL) {
                // PostgreSQL: Check if database exists, create if not
                // Note: PostgreSQL doesn't support IF NOT EXISTS in CREATE DATABASE
                // So we check first
                java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"
                );
                if (!rs.next()) {
                    // Database doesn't exist, create it
                    stmt.executeUpdate("CREATE DATABASE " + dbName + " WITH ENCODING 'UTF8'");
                    AppLogger.info("Database '" + dbName + "' created");
                } else {
                    AppLogger.info("Database '" + dbName + "' already exists");
                }
                rs.close();
            } else {
                // MySQL
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + 
                                 " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                AppLogger.info("Database '" + dbName + "' created or already exists");
            }
            
            stmt.close();
            conn.close();
            return true;
        } catch (Exception e) {
            AppLogger.error("Failed to create database", e);
            return false;
        }
    }

    /**
     * Extract database name from URL
     */
    private static String extractDatabaseName(String url) {
        // jdbc:mysql://localhost:3306/blue_moon?useSSL=false...
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1) {
            return "blue_moon";
        }
        
        String dbPart = url.substring(lastSlash + 1);
        int questionMark = dbPart.indexOf('?');
        if (questionMark != -1) {
            dbPart = dbPart.substring(0, questionMark);
        }
        
        return dbPart.isEmpty() ? "blue_moon" : dbPart;
    }

    /**
     * Create tables by running schema.sql or schema-postgresql.sql
     */
    private static void createTablesIfNotExists() throws Exception {
        String driver = dbConfig.getDriver();
        boolean isPostgreSQL = driver.contains("postgresql");
        String schemaFile = isPostgreSQL ? "sql/schema-postgresql.sql" : "sql/schema.sql";
        
        String schema;
        try (InputStream schemaStream = DatabaseInitializer.class
            .getClassLoader()
            .getResourceAsStream(schemaFile)) {
            
            if (schemaStream == null) {
                throw new RuntimeException("Cannot find " + schemaFile + " file");
            }

            schema = new BufferedReader(
                new InputStreamReader(schemaStream, StandardCharsets.UTF_8)
            ).lines()
            .collect(Collectors.joining("\n"));
        }

        // Remove CREATE DATABASE and USE statements as we already have the connection
        schema = schema.replaceAll("(?i)CREATE DATABASE.*?;", "");
        schema = schema.replaceAll("(?i)USE\\s+\\w+\\s*;", "");

        // Split by semicolon and execute each statement
        String[] statements = schema.split(";");
        
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    try {
                        stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        // Ignore "table already exists" errors
                        if (!e.getMessage().contains("already exists")) {
                            AppLogger.warn("Error executing SQL: " + sql.substring(0, Math.min(50, sql.length())) + "... - " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove old unique constraint on fee_collections to allow multiple fees per month/year
     */
    private static void removeOldUniqueConstraint() {
        try {
            Connection conn = JdbcUtils.getConnection();
            try {
                Statement stmt = conn.createStatement();
                
                String driver = dbConfig.getDriver();
                boolean isPostgreSQL = driver.contains("postgresql");
                
                if (isPostgreSQL) {
                    // PostgreSQL: Drop index and constraints
                    try {
                        stmt.executeUpdate("DROP INDEX IF EXISTS idx_fee_collections_household_month_year CASCADE");
                        AppLogger.info("Dropped unique index idx_fee_collections_household_month_year");
                    } catch (SQLException e) {
                        // Ignore if doesn't exist
                        if (!e.getMessage().contains("does not exist")) {
                            AppLogger.warn("Error dropping index: " + e.getMessage());
                        }
                    }
                    
                    try {
                        stmt.executeUpdate("ALTER TABLE fee_collections DROP CONSTRAINT IF EXISTS fee_collections_household_id_month_year_key CASCADE");
                    } catch (SQLException e) {
                        // Ignore if doesn't exist
                    }
                    
                    try {
                        stmt.executeUpdate("ALTER TABLE fee_collections DROP CONSTRAINT IF EXISTS idx_fee_collections_household_month_year CASCADE");
                    } catch (SQLException e) {
                        // Ignore if doesn't exist
                    }
                    
                    // Check if fee_type_id column exists and create new unique constraint if it does
                    try {
                        java.sql.ResultSet rs = stmt.executeQuery(
                            "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_name = 'fee_collections' AND column_name = 'fee_type_id'"
                        );
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Create new unique constraint with fee_type_id
                            try {
                                stmt.executeUpdate(
                                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_fee_collections_household_month_year_fee_type " +
                                    "ON fee_collections(household_id, month, year, fee_type_id) " +
                                    "WHERE month IS NOT NULL AND year IS NOT NULL AND fee_type_id IS NOT NULL"
                                );
                                AppLogger.info("Created unique index with fee_type_id");
                            } catch (SQLException e) {
                                // Ignore if already exists
                                if (!e.getMessage().contains("already exists")) {
                                    AppLogger.warn("Error creating new unique index: " + e.getMessage());
                                }
                            }
                        }
                    } catch (SQLException e) {
                        AppLogger.warn("Error checking for fee_type_id column: " + e.getMessage());
                    }
                } else {
                    // MySQL: Drop index
                    try {
                        stmt.executeUpdate("DROP INDEX idx_fee_collections_household_month_year ON fee_collections");
                        AppLogger.info("Dropped unique index idx_fee_collections_household_month_year");
                    } catch (SQLException e) {
                        // Ignore if doesn't exist
                        if (!e.getMessage().contains("Unknown key") && !e.getMessage().contains("doesn't exist")) {
                            AppLogger.warn("Error dropping index: " + e.getMessage());
                        }
                    }
                }
                
                stmt.close();
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            AppLogger.warn("Error removing old unique constraint: " + e.getMessage());
            // Don't throw exception, just log warning
        } catch (DbException e) {
            AppLogger.warn("Error getting connection to remove old unique constraint: " + e.getMessage());
            // Don't throw exception, just log warning
        }
    }
    
    /**
     * Check if database is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}