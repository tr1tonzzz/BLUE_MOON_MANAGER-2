package vn.bluemoon.model.entity;

import java.time.LocalDate;

/**
 * Entity for Resident (Nhân khẩu)
 */
public class Resident {
    private Integer id;
    private Integer householdId;
    private Integer userId;  // Liên kết với users
    private String fullName;
    private String idCard;
    private LocalDate dateOfBirth;
    private String gender;
    private String relationship;
    private String phone;
    private String email;
    private String occupation;
    private String permanentAddress;
    private String temporaryAddress;
    private String status;
    private String notes;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    // Tạm trú/Tạm vắng
    private LocalDate temporaryResidentFrom;
    private LocalDate temporaryResidentTo;
    private LocalDate temporaryAbsentFrom;
    private LocalDate temporaryAbsentTo;
    private String temporaryReason;
    
    // Thông tin từ household và apartment (join)
    private String apartmentCode;
    private String householdCode;
    private String ownerName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(Integer householdId) {
        this.householdId = householdId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getTemporaryAddress() {
        return temporaryAddress;
    }

    public void setTemporaryAddress(String temporaryAddress) {
        this.temporaryAddress = temporaryAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getApartmentCode() {
        return apartmentCode;
    }

    public void setApartmentCode(String apartmentCode) {
        this.apartmentCode = apartmentCode;
    }

    public String getHouseholdCode() {
        return householdCode;
    }

    public void setHouseholdCode(String householdCode) {
        this.householdCode = householdCode;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public LocalDate getTemporaryResidentFrom() {
        return temporaryResidentFrom;
    }
    
    public void setTemporaryResidentFrom(LocalDate temporaryResidentFrom) {
        this.temporaryResidentFrom = temporaryResidentFrom;
    }
    
    public LocalDate getTemporaryResidentTo() {
        return temporaryResidentTo;
    }
    
    public void setTemporaryResidentTo(LocalDate temporaryResidentTo) {
        this.temporaryResidentTo = temporaryResidentTo;
    }
    
    public LocalDate getTemporaryAbsentFrom() {
        return temporaryAbsentFrom;
    }
    
    public void setTemporaryAbsentFrom(LocalDate temporaryAbsentFrom) {
        this.temporaryAbsentFrom = temporaryAbsentFrom;
    }
    
    public LocalDate getTemporaryAbsentTo() {
        return temporaryAbsentTo;
    }
    
    public void setTemporaryAbsentTo(LocalDate temporaryAbsentTo) {
        this.temporaryAbsentTo = temporaryAbsentTo;
    }
    
    public String getTemporaryReason() {
        return temporaryReason;
    }
    
    public void setTemporaryReason(String temporaryReason) {
        this.temporaryReason = temporaryReason;
    }
    
    public String getGenderDisplay() {
        if (gender == null) return "";
        switch (gender.toLowerCase()) {
            case "male": return "Nam";
            case "female": return "Nữ";
            default: return gender;
        }
    }
    
    public String getStatusDisplay() {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "active": return "Đang ở";
            case "temporary_resident": return "Tạm trú";
            case "temporary_absent": return "Tạm vắng";
            case "moved_out": return "Đã chuyển đi";
            case "deceased": return "Đã mất";
            default: return status;
        }
    }
}


