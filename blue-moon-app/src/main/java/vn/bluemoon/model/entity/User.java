package vn.bluemoon.model.entity;

import java.time.LocalDateTime;

/**
 * Entity class for User
 */
public class User {
    private Integer id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String address;
    private Boolean isActive;
    private Boolean mustChangePassword;
    private java.time.LocalDate passwordChangeRequiredDate; // Ngày yêu cầu đổi mật khẩu
    private Integer passwordChangePeriodDays; // Số ngày định kỳ phải đổi mật khẩu
    private java.time.LocalDate lastPasswordChangeDate; // Ngày đổi mật khẩu lần cuối
    private String facebookId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(Integer id, String username, String email, String passwordHash, String fullName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isActive = true;
        this.mustChangePassword = false;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public java.time.LocalDate getPasswordChangeRequiredDate() {
        return passwordChangeRequiredDate;
    }

    public void setPasswordChangeRequiredDate(java.time.LocalDate passwordChangeRequiredDate) {
        this.passwordChangeRequiredDate = passwordChangeRequiredDate;
    }

    public Integer getPasswordChangePeriodDays() {
        return passwordChangePeriodDays;
    }

    public void setPasswordChangePeriodDays(Integer passwordChangePeriodDays) {
        this.passwordChangePeriodDays = passwordChangePeriodDays;
    }

    public java.time.LocalDate getLastPasswordChangeDate() {
        return lastPasswordChangeDate;
    }

    public void setLastPasswordChangeDate(java.time.LocalDate lastPasswordChangeDate) {
        this.lastPasswordChangeDate = lastPasswordChangeDate;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}






