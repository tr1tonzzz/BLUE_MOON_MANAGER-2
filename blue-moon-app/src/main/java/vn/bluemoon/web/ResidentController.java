package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.ResidentService;
import vn.bluemoon.validation.ValidationException;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

/**
 * Web controller for resident management
 */
@Controller
public class ResidentController {
    
    private final ResidentService residentService = new ResidentService();
    
    @GetMapping("/residents")
    public String residentsPage(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String apartmentCode,
            @RequestParam(required = false) String householdCode) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền quản lý nhân khẩu
        try {
            if (!vn.bluemoon.security.Authorization.canManageResidents(user)) {
                model.addAttribute("error", "Bạn không có quyền truy cập chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            List<vn.bluemoon.model.entity.Resident> residents;
            if ((name != null && !name.isEmpty()) || 
                (apartmentCode != null && !apartmentCode.isEmpty()) ||
                (householdCode != null && !householdCode.isEmpty())) {
                residents = residentService.searchResidents(
                    name != null ? name : "",
                    apartmentCode != null ? apartmentCode : "",
                    householdCode != null ? householdCode : ""
                );
            } else {
                residents = residentService.getAllResidents();
            }
            
            // Lấy danh sách households để chọn khi thêm nhân khẩu
            vn.bluemoon.repository.ResidentRepository residentRepo = new vn.bluemoon.repository.ResidentRepository();
            List<vn.bluemoon.model.entity.Resident> households = residentRepo.findAll();
            
            model.addAttribute("residents", residents != null ? residents : new java.util.ArrayList<>());
            model.addAttribute("households", households != null ? households : new java.util.ArrayList<>());
            model.addAttribute("user", user);
            model.addAttribute("searchName", name != null ? name : "");
            model.addAttribute("searchApartmentCode", apartmentCode != null ? apartmentCode : "");
            model.addAttribute("searchHouseholdCode", householdCode != null ? householdCode : "");
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("residents", new java.util.ArrayList<>());
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi không xác định: " + e.getMessage());
            model.addAttribute("residents", new java.util.ArrayList<>());
            e.printStackTrace();
        }
        
        return "residents";
    }
    
    @GetMapping("/residents/detail")
    public String residentDetail(
            HttpSession session,
            Model model,
            @RequestParam Integer id) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền xem chi tiết nhân khẩu
        try {
            if (!vn.bluemoon.security.Authorization.canManageResidents(user)) {
                model.addAttribute("error", "Bạn không có quyền truy cập chức năng này");
                return "redirect:/main";
            }
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/main";
        }
        
        try {
            vn.bluemoon.model.entity.Resident resident = residentService.getResidentById(id);
            if (resident == null) {
                model.addAttribute("error", "Không tìm thấy cư dân");
                return "redirect:/residents";
            }
            
            model.addAttribute("resident", resident);
            model.addAttribute("user", user);
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải thông tin: " + e.getMessage());
            return "redirect:/residents";
        }
        
        return "resident-detail";
    }
    
    @PostMapping("/residents/register-temporary-resident")
    public String registerTemporaryResident(
            HttpSession session,
            @RequestParam Integer residentId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            residentService.registerTemporaryResident(residentId, from, to, reason);
            redirectAttributes.addFlashAttribute("success", "Đã đăng ký tạm trú thành công");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi định dạng ngày tháng");
        }
        
        return "redirect:/residents/detail?id=" + residentId;
    }
    
    @PostMapping("/residents/register-temporary-absent")
    public String registerTemporaryAbsent(
            HttpSession session,
            @RequestParam Integer residentId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            residentService.registerTemporaryAbsent(residentId, from, to, reason);
            redirectAttributes.addFlashAttribute("success", "Đã đăng ký tạm vắng thành công");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi định dạng ngày tháng");
        }
        
        return "redirect:/residents/detail?id=" + residentId;
    }
    
    @PostMapping("/residents/cancel-temporary")
    public String cancelTemporaryStatus(
            HttpSession session,
            @RequestParam Integer residentId,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            residentService.cancelTemporaryStatus(residentId);
            redirectAttributes.addFlashAttribute("success", "Đã hủy tạm trú/tạm vắng thành công");
        } catch (ValidationException | DbException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/residents/detail?id=" + residentId;
    }
    
    @PostMapping("/residents/delete")
    public String deleteResident(
            HttpSession session,
            @RequestParam Integer id,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền xóa nhân khẩu
        try {
            if (!vn.bluemoon.security.Authorization.canManageResidents(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/residents";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/residents";
        }
        
        try {
            residentService.deleteResident(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa nhân khẩu và toàn bộ dữ liệu liên quan thành công");
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa nhân khẩu: " + e.getMessage());
        }
        
        return "redirect:/residents";
    }
    
    @PostMapping("/residents/add")
    public String addResident(
            HttpSession session,
            @RequestParam(required = false) Integer householdId,
            @RequestParam(required = false) String apartmentCode,
            @RequestParam(required = false) String householdCode,
            @RequestParam String fullName,
            @RequestParam String idCard,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam String relationship,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String permanentAddress,
            @RequestParam(required = false) String temporaryAddress,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Admin và Tổ trưởng có quyền thêm nhân khẩu
        try {
            if (!vn.bluemoon.security.Authorization.canManageResidents(user)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/residents";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/residents";
        }
        
        try {
            // Validate relationship
            if (relationship == null || relationship.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn quan hệ với chủ hộ");
                return "redirect:/residents";
            }
            
            boolean isChuHo = "Chủ hộ".equals(relationship.trim());
            
            // If "Chủ hộ", need to create new household
            Integer finalHouseholdId = householdId;
            if (isChuHo) {
                // Validate apartment code and household code
                if (apartmentCode == null || apartmentCode.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Mã căn hộ không được để trống khi thêm chủ hộ");
                    return "redirect:/residents";
                }
                if (householdCode == null || householdCode.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Mã hộ không được để trống khi thêm chủ hộ");
                    return "redirect:/residents";
                }
                
                // Create new household
                finalHouseholdId = residentService.createHousehold(apartmentCode.trim(), householdCode.trim(), fullName, phone, email);
            } else {
                // For non-"Chủ hộ", must have householdId
                if (householdId == null) {
                    redirectAttributes.addFlashAttribute("error", "Vui lòng chọn hộ dân");
                    return "redirect:/residents";
                }
            }
            
            // Parse date of birth
            LocalDate dob = null;
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    dob = LocalDate.parse(dateOfBirth);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Ngày sinh không hợp lệ. Vui lòng nhập theo định dạng YYYY-MM-DD");
                    return "redirect:/residents";
                }
            }
            
            // Set default status
            if (status == null || status.trim().isEmpty()) {
                status = "active";
            }
            
            residentService.createResident(
                finalHouseholdId,
                fullName,
                idCard,
                dob,
                gender,
                relationship,
                phone,
                email,
                occupation,
                permanentAddress,
                temporaryAddress,
                status
            );
            
            redirectAttributes.addFlashAttribute("success", "Đã thêm nhân khẩu thành công");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thêm nhân khẩu: " + e.getMessage());
        }
        
        return "redirect:/residents";
    }
}

