package vn.bluemoon.model.entity;

import java.time.LocalDateTime;

/**
 * Entity class for Role (User-Role relationship)
 */
public class Role {
    private Integer id;
    private Integer userId;
    private Integer groupId;
    private LocalDateTime assignedAt;

    public Role() {
    }

    public Role(Integer userId, Integer groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}

















