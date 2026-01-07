package vn.bluemoon.service;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.repository.FeeCollectionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for payment processing
 */
public class PaymentService {
    private final FeeCollectionRepository feeRepository = new FeeCollectionRepository();
    
    /**
     * Get all unpaid fee collections for a user
     */
    public List<FeeCollection> getUnpaidFeesForUser(Integer userId) throws DbException {
        List<FeeCollection> allFees = feeRepository.findByUserId(userId);
        // Lọc các fee chưa thanh toán đủ (unpaid, partial_paid, hoặc overpaid để hiển thị)
        java.util.List<FeeCollection> result = new java.util.ArrayList<>();
        for (FeeCollection fee : allFees) {
            String status = fee.getStatus();
            if ("unpaid".equals(status) || "partial_paid".equals(status) || "overpaid".equals(status)) {
                result.add(fee);
            }
        }
        return result;
    }
    
    /**
     * Get total remaining amount for a user
     */
    public BigDecimal getTotalRemainingAmount(Integer userId) throws DbException {
        List<FeeCollection> unpaidFees = getUnpaidFeesForUser(userId);
        return unpaidFees.stream()
            .map(FeeCollection::getRemainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Process payment for a fee collection
     * @param feeId Fee collection ID
     * @param paymentAmount Amount to pay
     * @param paymentMethod Payment method
     * @return Updated fee collection
     */
    public FeeCollection processPayment(Integer feeId, BigDecimal paymentAmount, String paymentMethod) 
            throws DbException {
        
        FeeCollection fee = feeRepository.findById(feeId);
        if (fee == null) {
            throw new DbException("Không tìm thấy bản ghi thu phí");
        }
        
        BigDecimal currentPaid = fee.getPaidAmount();
        BigDecimal newPaidAmount = currentPaid.add(paymentAmount);
        BigDecimal totalAmount = fee.getAmount();
        BigDecimal remaining = totalAmount.subtract(newPaidAmount);
        
        // Cập nhật số tiền đã nộp
        fee.setPaidAmount(newPaidAmount);
        fee.setPaymentDate(LocalDate.now());
        fee.setPaymentMethod(paymentMethod);
        
        // Xác định trạng thái dựa trên số tiền
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            // Đã thanh toán đủ
            fee.setStatus("paid");
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // Còn thiếu
            fee.setStatus("partial_paid");
        } else {
            // Nộp dư (remaining < 0)
            fee.setStatus("overpaid");
        }
        
        feeRepository.update(fee);
        return fee;
    }
    
    /**
     * Find fee collection by ID
     */
    public FeeCollection getFeeById(Integer feeId) throws DbException {
        return feeRepository.findById(feeId);
    }
}

