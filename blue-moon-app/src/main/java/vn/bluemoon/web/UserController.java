package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.dto.UserSearchRequest;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.UserService;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Web controller for user management
 */
@Controller
public class UserController {
    
    private final UserService userService = new UserService();
    
    @GetMapping("/users")
    public String usersPage(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone) {
        
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            UserSearchRequest request = new UserSearchRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setFullName(fullName);
            request.setPhone(phone);
            
            List<User> users = userService.searchUsers(request);
            
            model.addAttribute("users", users);
            model.addAttribute("user", currentUser);
            model.addAttribute("searchUsername", username != null ? username : "");
            model.addAttribute("searchEmail", email != null ? email : "");
            model.addAttribute("searchFullName", fullName != null ? fullName : "");
            model.addAttribute("searchPhone", phone != null ? phone : "");
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        
        return "users";
    }
    
    @PostMapping("/users/delete")
    public String deleteUser(
            HttpSession session,
            @RequestParam Integer id,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Admin có quyền xóa người dùng
        try {
            if (!vn.bluemoon.security.Authorization.canManageUsers(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện chức năng này");
                return "redirect:/users";
            }
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi kiểm tra quyền: " + e.getMessage());
            return "redirect:/users";
        }
        
        // Không cho phép xóa chính mình
        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể xóa chính mình");
            return "redirect:/users";
        }
        
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa người dùng và toàn bộ dữ liệu liên quan thành công");
        } catch (DbException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa người dùng: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
}

