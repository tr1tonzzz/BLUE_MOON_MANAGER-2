package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeType;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.repository.FeeTypeRepository;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.service.FeeCollectionService;
import vn.bluemoon.validation.ValidationException;
import vn.bluemoon.validation.Validators;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Fee Type management
 */
public class FeeTypeService {
    private final FeeTypeRepository feeTypeRepository = new FeeTypeRepository();
    
    /**
     * Get all fee types
     */
    public List<FeeType> getAllFeeTypes() throws DbException {
        return feeTypeRepository.findAll();
    }
    
    /**
     * Get active fee types only
     */
    public List<FeeType> getActiveFeeTypes() throws DbException {
        return feeTypeRepository.findActive();
    }
    
    /**
     * Get fee type by ID
     */
    public FeeType getFeeTypeById(Integer id) throws DbException {
        return feeTypeRepository.findById(id);
    }
    
    /**
     * Create new fee type
     */
    public FeeType createFeeType(String name, String description, BigDecimal defaultAmount, Boolean isActive) 
            throws DbException, ValidationException {
        Validators.validateRequired(name, "Tên khoản thu");
        
        // Check if name already exists
        List<FeeType> existing = feeTypeRepository.findAll();
        for (FeeType ft : existing) {
            if (name.equals(ft.getName())) {
                throw new ValidationException("Tên khoản thu đã tồn tại");
            }
        }
        
        FeeType feeType = new FeeType();
        feeType.setName(name);
        feeType.setDescription(description);
        feeType.setDefaultAmount(defaultAmount != null ? defaultAmount : BigDecimal.ZERO);
        feeType.setIsActive(isActive != null ? isActive : true);
        
        Integer id = feeTypeRepository.create(feeType);
        return feeTypeRepository.findById(id);
    }
    
    /**
     * Update fee type
     */
    public void updateFeeType(Integer id, String name, String description, BigDecimal defaultAmount, Boolean isActive) 
            throws DbException, ValidationException {
        Validators.validateRequired(name, "Tên khoản thu");
        
        FeeType feeType = feeTypeRepository.findById(id);
        if (feeType == null) {
            throw new ValidationException("Không tìm thấy khoản thu");
        }
        
        // Check if name already exists (excluding current)
        List<FeeType> existing = feeTypeRepository.findAll();
        for (FeeType ft : existing) {
            if (name.equals(ft.getName()) && !id.equals(ft.getId())) {
                throw new ValidationException("Tên khoản thu đã tồn tại");
            }
        }
        
        feeType.setName(name);
        feeType.setDescription(description);
        feeType.setDefaultAmount(defaultAmount != null ? defaultAmount : BigDecimal.ZERO);
        feeType.setIsActive(isActive != null ? isActive : true);
        
        feeTypeRepository.update(feeType);
    }
    
    /**
     * Delete fee type (hard delete - chỉ xóa nếu không có fee collection nào đang sử dụng)
     */
    public void deleteFeeType(Integer id) throws DbException, ValidationException {
        System.out.println("=== DEBUG: FeeTypeService.deleteFeeType called ===");
        System.out.println("DEBUG: id=" + id);
        
        System.out.println("DEBUG: Finding fee type by id...");
        FeeType feeType = feeTypeRepository.findById(id);
        if (feeType == null) {
            System.out.println("DEBUG: Fee type not found with id=" + id);
            throw new ValidationException("Không tìm thấy khoản thu");
        }
        System.out.println("DEBUG: Found fee type: " + feeType.getName() + " (ID: " + feeType.getId() + ")");
        
        // Kiểm tra xem có fee collection nào đang sử dụng fee type này không
        // (thông qua fee_collection_details nếu có, hoặc kiểm tra trong notes/description)
        // Vì hiện tại fee_collections không có trường fee_type_id trực tiếp,
        // nên chúng ta có thể hard delete an toàn
        
        // Hard delete
        System.out.println("DEBUG: Executing DELETE FROM fee_types WHERE id = " + id);
        String sql = "DELETE FROM fee_types WHERE id = ?";
        try (java.sql.Connection conn = vn.bluemoon.util.JdbcUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            System.out.println("DEBUG: Executing update...");
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG: Rows affected: " + rowsAffected);
            if (rowsAffected == 0) {
                System.out.println("DEBUG: No rows affected, fee type may not exist");
                throw new ValidationException("Không thể xóa khoản thu");
            }
            System.out.println("DEBUG: Fee type deleted successfully");
        } catch (java.sql.SQLException e) {
            System.out.println("DEBUG: SQLException occurred: " + e.getMessage());
            e.printStackTrace();
            throw new DbException("Error deleting fee type: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo fee collection cho tất cả hộ dân với khoản thu này
     */
    public int collectFeeForAllHouseholds(Integer feeTypeId, Integer month, Integer year, java.time.LocalDate paymentDeadline) 
            throws DbException, ValidationException {
        FeeType feeType = feeTypeRepository.findById(feeTypeId);
        if (feeType == null) {
            throw new ValidationException("Không tìm thấy khoản thu");
        }
        
        if (!feeType.getIsActive()) {
            throw new ValidationException("Khoản thu đã bị ngừng áp dụng");
        }
        
        // Lấy tất cả các hộ dân (chủ hộ)
        ResidentRepository residentRepository = new ResidentRepository();
        List<Resident> households = residentRepository.findAll().stream()
            .filter(r -> "Chủ hộ".equals(r.getRelationship()))
            .collect(Collectors.toList());
        
        if (households.isEmpty()) {
            throw new ValidationException("Không có hộ dân nào để thu phí");
        }
        
        // Tạo fee collection cho mỗi hộ dân
        FeeCollectionService feeCollectionService = new FeeCollectionService();
        int successCount = 0;
        int failCount = 0;
        
        for (Resident resident : households) {
            try {
                // Kiểm tra xem đã có fee collection cho hộ này trong tháng/năm này với cùng fee_type_id chưa
                // Cho phép thu phí cùng tháng/năm nhưng khác loại phí dịch vụ
                if (!feeCollectionService.feeCollectionExists(resident.getHouseholdId(), month, year, feeTypeId)) {
                    feeCollectionService.createFeeCollection(
                        resident.getHouseholdId(), 
                        month, 
                        year, 
                        feeType.getDefaultAmount(),
                        feeTypeId,
                        paymentDeadline
                    );
                    successCount++;
                } else {
                    failCount++; // Đã tồn tại cùng fee_type_id
                }
            } catch (Exception e) {
                failCount++;
                System.err.println("Lỗi khi tạo thu phí cho hộ " + resident.getHouseholdCode() + ": " + e.getMessage());
            }
        }
        
        if (successCount == 0 && failCount > 0) {
            throw new ValidationException("Tất cả các hộ dân đã có thu phí cho tháng/năm này");
        }
        
        return successCount;
    }
    
    /**
     * Backward compatibility: Collect fee for all households without payment deadline
     */
    public int collectFeeForAllHouseholds(Integer feeTypeId, Integer month, Integer year) 
            throws DbException, ValidationException {
        return collectFeeForAllHouseholds(feeTypeId, month, year, null);
    }
}

