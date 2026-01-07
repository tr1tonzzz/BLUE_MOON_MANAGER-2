package vn.bluemoon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import vn.bluemoon.util.DatabaseInitializer;

/**
 * Spring Boot main application class
 */
@SpringBootApplication
public class BlueMoonApplication extends SpringBootServletInitializer {
    
    public static void main(String[] args) {
        // Initialize database before starting Spring Boot
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo Database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        SpringApplication.run(BlueMoonApplication.class, args);
    }
}
