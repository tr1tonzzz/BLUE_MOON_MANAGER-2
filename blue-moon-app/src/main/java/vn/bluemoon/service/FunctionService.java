package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.FunctionUpsertRequest;
import vn.bluemoon.model.entity.Function;
import vn.bluemoon.model.entity.FunctionGroup;
import vn.bluemoon.repository.FunctionGroupRepository;
import vn.bluemoon.repository.FunctionRepository;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.util.List;

/**
 * Function service
 */
public class FunctionService {
    private final FunctionRepository functionRepository = new FunctionRepository();
    private final FunctionGroupRepository functionGroupRepository = new FunctionGroupRepository();

    /**
     * Get all functions
     * @return List of functions
     * @throws DbException if database error occurs
     */
    public List<Function> getAllFunctions() throws DbException {
        return functionRepository.findAll();
    }

    /**
     * Get all function groups
     * @return List of function groups
     * @throws DbException if database error occurs
     */
    public List<FunctionGroup> getAllFunctionGroups() throws DbException {
        return functionGroupRepository.findAll();
    }

    /**
     * Create function
     * @param request Function create request
     * @return Created function
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public Function createFunction(FunctionUpsertRequest request) throws ValidationException, DbException {
        // Validate input
        Validators.validateRequired(request.getName(), "Tên chức năng");
        Validators.validateRequired(request.getBoundaryClass(), "Lớp biên");
        
        if (request.getFunctionGroupId() == null) {
            throw new ValidationException("Nhóm chức năng không được để trống");
        }

        // Check if name already exists
        if (functionRepository.existsByName(request.getName(), null)) {
            throw new ValidationException("Tên chức năng đã tồn tại");
        }

        // Check if boundary class already exists
        if (functionRepository.existsByBoundaryClass(request.getBoundaryClass(), null)) {
            throw new ValidationException("Lớp biên đã tồn tại");
        }

        // Create function
        Function function = new Function();
        function.setName(request.getName());
        function.setFunctionGroupId(request.getFunctionGroupId());
        function.setBoundaryClass(request.getBoundaryClass());
        function.setDescription(request.getDescription());

        return functionRepository.create(function);
    }

    /**
     * Update function
     * @param request Function update request
     * @throws ValidationException if validation fails
     * @throws DbException if database error occurs
     */
    public void updateFunction(FunctionUpsertRequest request) throws ValidationException, DbException {
        if (request.getId() == null) {
            throw new ValidationException("ID chức năng không được để trống");
        }

        // Validate input
        Validators.validateRequired(request.getName(), "Tên chức năng");
        Validators.validateRequired(request.getBoundaryClass(), "Lớp biên");
        
        if (request.getFunctionGroupId() == null) {
            throw new ValidationException("Nhóm chức năng không được để trống");
        }

        // Check if name already exists (excluding current function)
        if (functionRepository.existsByName(request.getName(), request.getId())) {
            throw new ValidationException("Tên chức năng đã tồn tại");
        }

        // Check if boundary class already exists (excluding current function)
        if (functionRepository.existsByBoundaryClass(request.getBoundaryClass(), request.getId())) {
            throw new ValidationException("Lớp biên đã tồn tại");
        }

        // Update function
        Function function = functionRepository.findById(request.getId());
        if (function == null) {
            throw new ValidationException("Chức năng không tồn tại");
        }

        function.setName(request.getName());
        function.setFunctionGroupId(request.getFunctionGroupId());
        function.setBoundaryClass(request.getBoundaryClass());
        function.setDescription(request.getDescription());

        functionRepository.update(function);
    }

    /**
     * Delete function
     * @param id Function ID
     * @throws DbException if database error occurs
     */
    public void deleteFunction(Integer id) throws DbException {
        functionRepository.delete(id);
    }

    /**
     * Get function by ID
     * @param id Function ID
     * @return Function
     * @throws DbException if database error occurs
     */
    public Function getFunctionById(Integer id) throws DbException {
        return functionRepository.findById(id);
    }
}

















