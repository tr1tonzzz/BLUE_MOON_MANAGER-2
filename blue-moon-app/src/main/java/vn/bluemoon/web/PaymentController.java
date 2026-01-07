package vn.bluemoon.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.bluemoon.model.entity.User;

import javax.servlet.http.HttpSession;

/**
 * Web controller for payment
 */
@Controller
public class PaymentController {
    
    @GetMapping("/payment")
    public String paymentPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "payment";
    }
}





