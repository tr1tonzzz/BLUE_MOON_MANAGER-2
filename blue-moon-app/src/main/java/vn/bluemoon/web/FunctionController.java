package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.User;
import vn.bluemoon.service.FunctionService;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Web controller for function management
 */
@Controller
public class FunctionController {
    
    private final FunctionService functionService = new FunctionService();
    
    @GetMapping("/functions")
    public String functionsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            List<vn.bluemoon.model.entity.Function> functions = functionService.getAllFunctions();
            List<vn.bluemoon.model.entity.FunctionGroup> functionGroups = functionService.getAllFunctionGroups();
            
            model.addAttribute("functions", functions);
            model.addAttribute("functionGroups", functionGroups);
            model.addAttribute("user", user);
        } catch (DbException e) {
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }
        
        return "functions";
    }
}





