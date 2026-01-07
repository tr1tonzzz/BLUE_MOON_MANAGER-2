package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.Resident;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.PersonalInfoService;
import vn.bluemoon.validation.ValidationException;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;

/**
 * Web controller for personal information
 */
@Controller
public class PersonalController {
    
    private final PersonalInfoService personalInfoService = new PersonalInfoService();
    
    @GetMapping("/personal-info")
    public String personalInfoPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            // Kiểm tra xem user đã có resident record chưa
            Resident resident = personalInfoService.getPersonalInfo(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("resident", resident);
            model.addAttribute("hasResident", resident != null);
        } catch (DbException e) {
            model.addAttribute("user", user);
            model.addAttribute("resident", null);
            model.addAttribute("hasResident", false);
            model.addAttribute("error", "Lỗi khi tải thông tin: " + e.getMessage());
        }
        
        return "personal-info";
    }
    
    @PostMapping("/personal-info/register")
    public String registerPersonalInfo(
            HttpSession session,
            @RequestParam String fullName,
            @RequestParam String idCard,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String apartmentCode,
            @RequestParam(required = false) String householdCode,
            RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            vn.bluemoon.model.dto.PersonalInfoRequest request = new vn.bluemoon.model.dto.PersonalInfoRequest();
            request.setFullName(fullName);
            request.setIdCard(idCard);
            
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                request.setDateOfBirth(LocalDate.parse(dateOfBirth));
            }
            
            request.setGender(gender);
            request.setPhone(phone);
            request.setEmail(email);
            request.setOccupation(occupation);
            request.setApartmentCode(apartmentCode);
            request.setHouseholdCode(householdCode);
            request.setStatus("active");
            
            personalInfoService.registerOrUpdatePersonalInfo(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thông tin cá nhân thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi định dạng dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/personal-info";
    }
}

