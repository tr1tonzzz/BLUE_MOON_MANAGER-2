package vn.bluemoon.model.dto;

/**
 * DTO for function create/update request
 */
public class FunctionUpsertRequest {
    private Integer id; // For update
    private String name;
    private Integer functionGroupId;
    private String boundaryClass;
    private String description;

    public FunctionUpsertRequest() {
    }

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
}

















