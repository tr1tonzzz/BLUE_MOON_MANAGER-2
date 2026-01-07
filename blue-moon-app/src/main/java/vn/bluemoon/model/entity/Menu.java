package vn.bluemoon.model.entity;

import java.time.LocalDateTime;

/**
 * Entity class for Menu
 */
public class Menu {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer functionId;
    private Integer displayOrder;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Menu() {
    }

    public Menu(String name, Integer parentId, Integer functionId, Integer displayOrder) {
        this.name = name;
        this.parentId = parentId;
        this.functionId = functionId;
        this.displayOrder = displayOrder;
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

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Integer functionId) {
        this.functionId = functionId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
        return "Menu{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                ", functionId=" + functionId +
                ", displayOrder=" + displayOrder +
                '}';
    }
}

















