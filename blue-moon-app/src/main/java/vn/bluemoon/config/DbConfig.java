package vn.bluemoon.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Database configuration
 */
public class DbConfig {
    private static DbConfig instance;
    private Properties properties;

    private DbConfig() {
        loadProperties();
    }

    public static DbConfig getInstance() {
        if (instance == null) {
            instance = new DbConfig();
        }
        return instance;
    }

    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("application.properties not found");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Error loading application.properties", e);
        }
    }

    public String getUrl() {
        // Try Spring Boot properties first, then legacy properties
        String url = getProperty("spring.datasource.url");
        if (url == null || url.isEmpty() || url.startsWith("${")) {
            url = getProperty("db.url");
        }
        if (url == null || url.isEmpty() || url.startsWith("${")) {
            url = "jdbc:mysql://localhost:3306/blue_moon";
        }
        return url;
    }

    public String getUsername() {
        // Try Spring Boot properties first, then legacy properties
        String username = getProperty("spring.datasource.username");
        if (username == null || username.isEmpty() || username.startsWith("${")) {
            username = getProperty("db.username");
        }
        if (username == null || username.isEmpty() || username.startsWith("${")) {
            username = "root";
        }
        return username;
    }

    public String getPassword() {
        // Try Spring Boot properties first, then legacy properties
        String password = getProperty("spring.datasource.password");
        if (password == null || password.startsWith("${")) {
            password = getProperty("db.password");
        }
        if (password == null || password.startsWith("${")) {
            password = "";
        }
        return password;
    }

    public String getDriver() {
        // Try Spring Boot properties first, then legacy properties
        String driver = getProperty("spring.datasource.driver-class-name");
        if (driver == null || driver.isEmpty() || driver.startsWith("${")) {
            driver = getProperty("db.driver");
        }
        if (driver == null || driver.isEmpty() || driver.startsWith("${")) {
            driver = "com.mysql.cj.jdbc.Driver";
        }
        return driver;
    }

    private String getProperty(String key) {
        return properties.getProperty(key);
    }

    private String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value == null || value.isEmpty() || value.startsWith("${")) ? defaultValue : value;
    }
}













