package vn.bluemoon.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration
 */
public class AppConfig {
    private static AppConfig instance;
    private Properties properties;

    private AppConfig() {
        loadProperties();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
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

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Email configuration
    public String getEmailSmtpHost() {
        return getProperty("email.smtp.host", "smtp.gmail.com");
    }

    public String getEmailSmtpPort() {
        return getProperty("email.smtp.port", "587");
    }

    public boolean getEmailSmtpAuth() {
        return Boolean.parseBoolean(getProperty("email.smtp.auth", "true"));
    }

    public boolean getEmailSmtpStartTls() {
        return Boolean.parseBoolean(getProperty("email.smtp.starttls.enable", "true"));
    }

    public String getEmailFrom() {
        return getProperty("email.from", "");
    }

    public String getEmailPassword() {
        return getProperty("email.from.password", "");
    }

    // Application configuration
    public String getAppName() {
        return getProperty("app.name", "Blue Moon Apartment Management System");
    }

    public String getAppVersion() {
        return getProperty("app.version", "1.0.0");
    }

    // Security configuration
    public int getPasswordResetTokenExpiryHours() {
        return Integer.parseInt(getProperty("password.reset.token.expiry.hours", "24"));
    }

    public int getSessionExpiryHours() {
        return Integer.parseInt(getProperty("session.expiry.hours", "8"));
    }
}

















