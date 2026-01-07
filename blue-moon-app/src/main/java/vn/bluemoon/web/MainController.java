package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.StatisticsService;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;

/**
 * Web controller for main dashboard
 */
@Controller
public class MainController {
    
    @GetMapping("/main")
    public String mainPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get user permissions by checking roles
        Set<String> permissions = new java.util.HashSet<>();
        try {
            // Check roles
            if (vn.bluemoon.security.Authorization.isAdmin(user)) {
                // Admin có tất cả quyền của Tổ trưởng và Kế toán
                permissions.add("USER_MANAGEMENT");
                permissions.add("RESIDENT_MANAGEMENT");
                permissions.add("FEE_MANAGEMENT");
                permissions.add("FEE_TYPE_MANAGEMENT");
                permissions.add("FUNCTION_MANAGEMENT");
                permissions.add("CAN_COLLECT_FEE");
                permissions.add("CAN_MANAGE_FEES"); // Quản lý thu phí
            } else if (vn.bluemoon.security.Authorization.hasRole(user, "Tổ trưởng")) {
                permissions.add("USER_MANAGEMENT");
                permissions.add("RESIDENT_MANAGEMENT");
                permissions.add("FEE_MANAGEMENT");
                permissions.add("FEE_TYPE_MANAGEMENT");
            } else if (vn.bluemoon.security.Authorization.hasRole(user, "Kế toán")) {
                permissions.add("FEE_MANAGEMENT");
                permissions.add("CAN_COLLECT_FEE");
            }
        } catch (vn.bluemoon.exception.DbException e) {
            // Log error but continue
        }
        
        // Get dashboard statistics
        try {
            StatisticsService statsService = new StatisticsService();
            Map<String, Object> stats = statsService.getDashboardStats();
            model.addAttribute("stats", stats);
        } catch (DbException e) {
            // Log error but continue
            model.addAttribute("statsError", "Không thể tải thống kê: " + e.getMessage());
        }
        
        model.addAttribute("user", user);
        model.addAttribute("permissions", permissions);
        
        return "main";
    }
}

