package vn.bluemoon.model.entity;

import java.time.LocalDateTime;

/**
 * Entity class for Function
 */
public class Function {
    private Integer id;
    private String name;
    private Integer functionGroupId;
    private String boundaryClass;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional field for display
    private String functionGroupName;

    public Function() {
    }

    public Function(String name, Integer functionGroupId, String boundaryClass) {
        this.name = name;
        this.functionGroupId = functionGroupId;
        this.boundaryClass = boundaryClass;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getFunctionGroupId() {
        return functionGroupId;
    }

    public void setFunctionGroupId(Integer functionGroupId) {
        this.functionGroupId = functionGroupId;
    }

    public String getBoundaryClass() {
        return boundaryClass;
    }

    public void setBoundaryClass(String boundaryClass) {
        this.boundaryClass = boundaryClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getFunctionGroupName() {
        return functionGroupName;
    }

    public void setFunctionGroupName(String functionGroupName) {
        this.functionGroupName = functionGroupName;
    }

    @Override
    public String toString() {
        return "Function{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", functionGroupId=" + functionGroupId +
                ", boundaryClass='" + boundaryClass + '\'' +
                '}';
    }
}

















