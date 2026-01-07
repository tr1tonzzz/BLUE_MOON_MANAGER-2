package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.FeeCollection;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.repository.ResidentRepository;
import vn.bluemoon.service.FeeCollectionService;
import vn.bluemoon.service.FeeTypeService;
import vn.bluemoon.validation.ValidationException;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Web controller for fee collection management
 */
@Controller
public class FeeController {
    
    private final FeeCollectionService feeService = new FeeCollectionService();
    private final ResidentRepository residentRepository = new ResidentRepository();
    
    @GetMapping("/fees")
    public String feesPage(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String apartmentCode,
            @RequestParam(required = false) String householdCode,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin có tất cả quyền (quản lý và thu phí)
        // Tổ trưởng có quyền quản lý, Kế toán có quyền thu phí
        boolean canManage = false;
        boolean canCollect = false;
        try {
            // Admin có tất cả quyền
            canManage = vn.bluemoon.security.Authorization.canManageFeeTypes(user);
            canCollect = vn.bluemoon.security.Authorization.canCollectFees(user);
        } catch (DbException e) {
            // Continue with default permissions
        }
        
        model.addAttribute("canManage", canManage);
        model.addAttribute("canCollect", canCollect);
        
        // Lấy danh sách các khoản thu để hiển thị trong phần "Thêm thu phí"
        try {
            FeeTypeService feeTypeService = new FeeTypeService();
            model.addAttribute("feeTypes", feeTypeService.getActiveFeeTypes());
        } catch (DbException e) {
            // Log error but continue
            model.addAttribute("feeTypes", new java.util.ArrayList<>());
        }
        
        // Lấy danh sách hộ dân (chủ hộ) để chọn thu phí theo hộ
        // findAll() đã filter relationship = 'Chủ hộ' rồi, không cần filter lại
        try {
            System.out.println("DEBUG: FeeController - Loading households for dropdown...");
            List<Resident> households = residentRepository.findAll();
            System.out.println("DEBUG: FeeController - Loaded " + households.size() + " households");
            for (int i = 0; i < households.size(); i++) {
                Resident h = households.get(i);
                System.out.println("DEBUG: Household #" + (i+1) + ": " + h.getFullName() + 
                                 " (ID: " + h.getId() + ", Household ID: " + h.getHouseholdId() + ")");
            }
            model.addAttribute("households", households);
        } catch (DbException e) {
            System.err.println("DEBUG: Error loading households: " + e.getMessage());
            e.printStackTrace();
            // Log error but continue
            model.addAttribute("households", new java.util.ArrayList<>());
        }
        
        try {
            List<FeeCollection> fees;
            boolean hasSearchCriteria = (apartmentCode != null && !apartmentCode.trim().isEmpty()) ||
                                       (householdCode != null && !householdCode.trim().isEmpty()) ||
                                       (ownerName != null && !ownerName.trim().isEmpty()) ||
                                       month != null ||
                                       year != null ||
                                       (status != null && !status.trim().isEmpty() && !status.equals("all"));
            
            if (!hasSearchCriteria) {
                fees = feeService.getAllFeeCollections();
            } else {
                // Convert status display to database value
                String statusValue = null;
                if (status != null && !status.trim().isEmpty() && !status.equals("all")) {
                    if (status.equals("paid")) {
                        statusValue = "paid";
                    } else if (status.equals("unpaid")) {
                        statusValue = "unpaid";
                    } else {
                        statusValue = status;
                    }
                }
                
                // Debug log
                System.out.println("DEBUG: FeeController.feesPage - Search parameters:");
                System.out.println("  apartmentCode: " + apartmentCode);
                System.out.println("  householdCode: " + householdCode);
                System.out.println("  ownerName: " + ownerName);
                System.out.println("  month: " + month);
                System.out.println("  year: " + year);
                System.out.println("  status (raw): " + status);
                System.out.println("  statusValue (converted): " + statusValue);
                
                fees = feeService.searchFeeCollections(
                    apartmentCode != null && !apartmentCode.trim().isEmpty() ? apartmentCode.trim() : null,
                    householdCode != null && !householdCode.trim().isEmpty() ? householdCode.trim() : null,
                    ownerName != null && !ownerName.trim().isEmpty() ? ownerName.trim() : null,
                    month,
                    year,
                    statusValue
                );
                
                System.out.println("DEBUG: FeeController.feesPage - Found " + fees.size() + " fee collections");
            }
            
            // Calculate statistics
            long total = fees.size();
            long paid = fees.stream().filter(f -> "paid".equals(f.getStatus())).count();
            long unpaid = total - paid;
            BigDecimal totalAmount = fees.stream()
                .map(f -> f.getAmount() != null ? f.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaidAmount = fees.stream()
                .map(f -> f.getPaidAmount() != null ? f.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // households đã được load ở trên (dòng 77-84), không cần load lại
            // Không filter theo user_id vì có thể có households không có user_id nhưng vẫn cần thu phí
            
            model.addAttribute("fees", fees);
            model.addAttribute("user", user);
            // households đã được add vào model ở dòng 84, không cần add lại
            model.addAttribute("searchApartmentCode", apartmentCode != null ? apartmentCode : "");
            model.addAttribute("searchHouseholdCode", householdCode != null ? householdCode : "");
            model.addAttribute("searchOwnerName", ownerName != null ? ownerName : "");
            model.addAttribute("searchMonth", month);
            model.addAttribute("searchYear", year); // Không set mặc định, để null nếu không chọn
            model.addAttribute("searchStatus", status != null ? status : "all");
            model.addAttribute("totalCount", total);
            model.addAttribute("paidCount", paid);
            model.addAttribute("unpaidCount", unpaid);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("totalPaidAmount", totalPaidAmount);
            
            // Years for dropdown
            int currentYear = LocalDate.now().getYear();
            model.addAttribute("years", java.util.stream.IntStream.rangeClosed(2020, currentYear + 1)
                .boxed().collect(Collectors.toList()));
            
            // Add permission flags
            model.addAttribute("canManage", canManage);
            model.addAttribute("canCollect", canCollect);
            
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        
        return "fees";
    }
    
    @PostMapping("/fees/add")
    public String addFee(
            @RequestParam(required = false) Integer householdId,
            @RequestParam(required = false) String selectAll,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam String amount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Chỉ Tổ trưởng và Quản trị viên mới có quyền tạo khoản thu
        try {
            if (!vn.bluemoon.security.Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/fees";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/fees";
        }
        
        try {
            BigDecimal amountValue = new BigDecimal(amount.replaceAll("[^0-9.]", ""));
            
            if ("on".equals(selectAll)) {
                // Create for all households
                // Không filter theo user_id vì có thể có households không có user_id nhưng vẫn cần thu phí
                List<Resident> households = residentRepository.findAll();
                
                int successCount = 0;
                int failCount = 0;
                
                for (Resident resident : households) {
                    try {
                        feeService.createFeeCollection(resident.getHouseholdId(), month, year, amountValue);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                    }
                }
                
                if (successCount > 0) {
                    redirectAttributes.addFlashAttribute("success", 
                        String.format("Đã tạo thu phí cho %d hộ dân", successCount) +
                        (failCount > 0 ? String.format(" (%d hộ dân thất bại)", failCount) : ""));
                } else {
                    redirectAttributes.addFlashAttribute("error", "Không thể tạo thu phí cho bất kỳ hộ dân nào");
                }
            } else {
                // Create for single household
                if (householdId == null) {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng chọn hộ dân");
                    return "redirect:/fees";
                }
                
                feeService.createFeeCollection(householdId, month, year, amountValue);
                redirectAttributes.addFlashAttribute("success", "Đã thêm thu phí thành công");
            }
            
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Số tiền không hợp lệ");
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo thu phí: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/fees";
    }
    
    @PostMapping("/fees/mark-paid")
    public String markAsPaid(
            @RequestParam Integer feeId,
            @RequestParam String paymentDate,
            @RequestParam String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Kế toán có quyền thu phí
        try {
            if (!vn.bluemoon.security.Authorization.canCollectFees(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thu phí");
                return "redirect:/fees";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/fees";
        }
        
        try {
            LocalDate date = LocalDate.parse(paymentDate);
            String method = paymentMethod.equals("Chuyển khoản") ? "bank_transfer" : 
                           paymentMethod.equals("Thẻ tín dụng") ? "credit_card" : "cash";
            
            feeService.markAsPaid(feeId, date, method);
            redirectAttributes.addFlashAttribute("success", "Đã đánh dấu đã thu phí thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đánh dấu đã thu phí: " + e.getMessage());
        }
        
        return "redirect:/fees";
    }
    
    @PostMapping("/fees/create-revenue")
    public String createRevenue(
            HttpSession session,
            @RequestParam String revenueCode,
            @RequestParam String revenueName,
            @RequestParam String revenueType,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Chỉ Tổ trưởng và Quản trị viên mới có quyền tạo khoản thu
        try {
            if (!vn.bluemoon.security.Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/fees";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/fees";
        }
        
        // Redirect to fee-types page instead
        return "redirect:/fee-types";
    }
    
    @PostMapping("/fees/collect-from-type")
    public String collectFeeFromType(
            HttpSession session,
            @RequestParam Integer feeTypeId,
            @RequestParam(required = false) Integer residentId,
            @RequestParam(required = false) Integer householdId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String paymentDeadline,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền
        try {
            if (!vn.bluemoon.security.Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/fees";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/fees";
        }
        
        // Nếu không có month/year, dùng tháng/năm hiện tại
        java.time.LocalDate now = java.time.LocalDate.now();
        int currentMonth = month != null ? month : now.getMonthValue();
        int currentYear = year != null ? year : now.getYear();
        
        try {
            FeeTypeService feeTypeService = new FeeTypeService();
            int successCount = 0;
            
            // Debug logs
            System.out.println("DEBUG: collectFeeFromType - feeTypeId=" + feeTypeId);
            System.out.println("DEBUG: collectFeeFromType - residentId=" + residentId);
            System.out.println("DEBUG: collectFeeFromType - householdId=" + householdId);
            System.out.println("DEBUG: collectFeeFromType - month=" + month + ", year=" + year);
            
            // Kiểm tra xem có chọn hộ dân cụ thể không
            // residentId và householdId phải có giá trị hợp lệ (> 0) để thu phí cho một hộ
            boolean hasSpecificHousehold = (residentId != null && residentId > 0) || (householdId != null && householdId > 0);
            
            System.out.println("DEBUG: collectFeeFromType - hasSpecificHousehold=" + hasSpecificHousehold);
            
            if (hasSpecificHousehold) {
                System.out.println("DEBUG: collectFeeFromType - Thu phí cho một hộ dân cụ thể");
                // Thu phí cho một hộ dân cụ thể
                vn.bluemoon.model.entity.FeeType feeType = feeTypeService.getFeeTypeById(feeTypeId);
                if (feeType == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy khoản thu");
                    return "redirect:/fees";
                }
                
                // Lấy householdId từ residentId nếu có
                Integer finalHouseholdId = householdId;
                if (residentId != null && residentId > 0) {
                    try {
                        vn.bluemoon.repository.ResidentRepository residentRepo = new vn.bluemoon.repository.ResidentRepository();
                        vn.bluemoon.model.entity.Resident resident = residentRepo.findById(residentId);
                        if (resident != null) {
                            finalHouseholdId = resident.getHouseholdId();
                            System.out.println("DEBUG: Creating fee for residentId=" + residentId + ", residentName=" + resident.getFullName() + ", householdId=" + finalHouseholdId);
                        } else {
                            // Nếu không tìm thấy resident, có thể residentId thực ra là householdId
                            // (khi không có resident record, template sẽ dùng householdId)
                            finalHouseholdId = residentId;
                            System.out.println("DEBUG: Resident not found, treating residentId=" + residentId + " as householdId");
                        }
                    } catch (vn.bluemoon.exception.DbException e) {
                        redirectAttributes.addFlashAttribute("error", "Lỗi khi lấy thông tin hộ dân: " + e.getMessage());
                        return "redirect:/fees";
                    }
                }
                
                // Nếu vẫn chưa có householdId, báo lỗi
                if (finalHouseholdId == null) {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng chọn hộ dân");
                    return "redirect:/fees";
                }
                
                FeeCollectionService feeCollectionService = new FeeCollectionService();
                // Kiểm tra duplicate trước khi tạo - chỉ kiểm tra cùng fee_type_id
                // Cho phép thu phí cùng tháng/năm nhưng khác loại phí dịch vụ
                if (feeCollectionService.feeCollectionExists(finalHouseholdId, currentMonth, currentYear, feeTypeId)) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Đã tồn tại thu phí cho hộ dân này trong tháng/năm này với cùng loại phí dịch vụ. Vui lòng chọn tháng/năm khác hoặc loại phí khác.");
                    return "redirect:/fees";
                }
                // Parse payment deadline nếu có
                java.time.LocalDate deadline = null;
                if (paymentDeadline != null && !paymentDeadline.isEmpty()) {
                    try {
                        deadline = java.time.LocalDate.parse(paymentDeadline);
                    } catch (Exception e) {
                        // Ignore parse error, deadline sẽ là null
                    }
                }
                
                System.out.println("DEBUG: collectFeeFromType - Creating fee for householdId=" + finalHouseholdId + 
                                 ", amount=" + feeType.getDefaultAmount() + ", feeTypeId=" + feeTypeId);
                feeCollectionService.createFeeCollection(finalHouseholdId, currentMonth, currentYear, feeType.getDefaultAmount(), feeTypeId, deadline);
                successCount = 1;
                redirectAttributes.addFlashAttribute("success", "Đã tạo thu phí thành công");
            } else {
                // Thu phí cho tất cả hộ dân
                System.out.println("DEBUG: collectFeeFromType - Thu phí cho TẤT CẢ hộ dân");
                // Parse payment deadline nếu có
                java.time.LocalDate deadline = null;
                if (paymentDeadline != null && !paymentDeadline.isEmpty()) {
                    try {
                        deadline = java.time.LocalDate.parse(paymentDeadline);
                    } catch (Exception e) {
                        // Ignore parse error, deadline sẽ là null
                    }
                }
                successCount = feeTypeService.collectFeeForAllHouseholds(feeTypeId, currentMonth, currentYear, deadline);
                redirectAttributes.addFlashAttribute("success", 
                    String.format("Đã tạo thu phí cho %d hộ dân thành công", successCount));
            }
            
            // Redirect với filter tháng/năm để xem kết quả
            return "redirect:/fees?month=" + currentMonth + "&year=" + currentYear;
        } catch (ValidationException | DbException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fees";
        }
    }
}

