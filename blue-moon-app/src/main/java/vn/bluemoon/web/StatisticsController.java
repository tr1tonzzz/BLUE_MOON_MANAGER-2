package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.FeeCollectionService;

import javax.servlet.http.HttpSession;

/**
 * Controller for Statistics page
 */
@Controller
public class StatisticsController {
    
    @GetMapping("/statistics")
    public String statisticsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Tất cả người dùng đã đăng nhập đều có thể xem thống kê
        // Tổ trưởng xem thống kê tổng hợp, Kế toán xem thống kê giới hạn
        boolean canViewFullStats = false;
        try {
            canViewFullStats = vn.bluemoon.security.Authorization.isAdmin(user) || 
                              vn.bluemoon.security.Authorization.canManageResidents(user);
        } catch (DbException e) {
            // Continue with default
        }
        
        try {
            FeeCollectionService feeService = new FeeCollectionService();
            
            // Get all fee collections for statistics table
            var allFees = feeService.getAllFeeCollections();
            
            model.addAttribute("user", user);
            model.addAttribute("fees", allFees);
            model.addAttribute("totalCount", allFees.size());
            model.addAttribute("paidCount", allFees.stream().filter(f -> "paid".equals(f.getStatus())).count());
            model.addAttribute("unpaidCount", allFees.stream().filter(f -> !"paid".equals(f.getStatus())).count());
            model.addAttribute("canViewFullStats", canViewFullStats);
            
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu thống kê: " + e.getMessage());
        }
        
        return "statistics";
    }
}

