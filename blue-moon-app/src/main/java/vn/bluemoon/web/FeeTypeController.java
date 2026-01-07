package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.security.Authorization;
import vn.bluemoon.service.FeeTypeService;
import vn.bluemoon.validation.ValidationException;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

/**
 * Controller for Fee Type management (chỉ Tổ trưởng mới có quyền)
 */
@Controller
public class FeeTypeController {
    
    private final FeeTypeService feeTypeService = new FeeTypeService();
    
    @GetMapping("/fee-types")
    public String feeTypesPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền quản lý khoản thu
        try {
            if (!Authorization.canManageFeeTypes(user)) {
                model.addAttribute("error", "Bạn không có quyền truy cập chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            model.addAttribute("feeTypes", feeTypeService.getAllFeeTypes());
            model.addAttribute("user", user);
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        
        return "fee-types";
    }
    
    @PostMapping("/fee-types/add")
    public String addFeeType(
            HttpSession session,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String defaultAmount,
            @RequestParam(required = false) Boolean isActive,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền
        try {
            if (!Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            BigDecimal amount = null;
            if (defaultAmount != null && !defaultAmount.trim().isEmpty()) {
                amount = new BigDecimal(defaultAmount.replace(",", "").replace(".", ""));
            }
            
            feeTypeService.createFeeType(name, description, amount, isActive);
            redirectAttributes.addFlashAttribute("success", "Đã tạo khoản thu thành công");
        } catch (ValidationException | DbException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi định dạng số tiền");
        }
        
        return "redirect:/fee-types";
    }
    
    @PostMapping("/fee-types/update")
    public String updateFeeType(
            HttpSession session,
            @RequestParam Integer id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String defaultAmount,
            @RequestParam(required = false) Boolean isActive,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền
        try {
            if (!Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            BigDecimal amount = null;
            if (defaultAmount != null && !defaultAmount.trim().isEmpty()) {
                amount = new BigDecimal(defaultAmount.replace(",", "").replace(".", ""));
            }
            
            feeTypeService.updateFeeType(id, name, description, amount, isActive);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật khoản thu thành công");
        } catch (ValidationException | DbException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi định dạng số tiền");
        }
        
        return "redirect:/fee-types";
    }
    
    @PostMapping("/fee-types/delete")
    public String deleteFeeType(
            HttpSession session,
            @RequestParam Integer id,
            RedirectAttributes redirectAttributes) {
        
        System.out.println("=== DEBUG: deleteFeeType endpoint called ===");
        System.out.println("DEBUG: Received id=" + id);
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("DEBUG: User is null, redirecting to login");
            return "redirect:/login";
        }
        System.out.println("DEBUG: User=" + user.getUsername() + " (ID: " + user.getId() + ")");
        
        // Admin và Tổ trưởng có quyền
        try {
            boolean canManage = Authorization.canManageFeeTypes(user);
            System.out.println("DEBUG: canManageFeeTypes=" + canManage);
            if (!canManage) {
                System.out.println("DEBUG: User does not have permission");
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            System.out.println("DEBUG: Error checking permission: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            System.out.println("DEBUG: Calling feeTypeService.deleteFeeType(" + id + ")");
            feeTypeService.deleteFeeType(id);
            System.out.println("DEBUG: Fee type deleted successfully");
            redirectAttributes.addFlashAttribute("success", "Đã xóa khoản thu thành công");
        } catch (ValidationException e) {
            System.out.println("DEBUG: ValidationException: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DbException e) {
            System.out.println("DEBUG: DbException: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        System.out.println("DEBUG: Redirecting to /fee-types");
        return "redirect:/fee-types";
    }
    
    @PostMapping("/fee-types/collect")
    public String collectFeeForAllHouseholds(
            HttpSession session,
            @RequestParam Integer feeTypeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền
        try {
            if (!Authorization.canManageFeeTypes(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        // Nếu không có month/year, dùng tháng/năm hiện tại
        java.time.LocalDate now = java.time.LocalDate.now();
        int currentMonth = month != null ? month : now.getMonthValue();
        int currentYear = year != null ? year : now.getYear();
        
        try {
            int successCount = feeTypeService.collectFeeForAllHouseholds(feeTypeId, currentMonth, currentYear);
            redirectAttributes.addFlashAttribute("success", 
                String.format("Đã tạo thu phí cho %d hộ dân thành công. Vui lòng kiểm tra trong Quản lý thu phí.", successCount));
            // Redirect đến trang quản lý thu phí để xem kết quả
            return "redirect:/fees?month=" + currentMonth + "&year=" + currentYear;
        } catch (ValidationException | DbException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/fee-types";
        }
    }
}

