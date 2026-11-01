package qltv.account;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Account> data = accountService.search(q, page);
        model.addAttribute("view", "accounts");
        model.addAttribute("data", data);
        model.addAttribute("q", q);
        model.addAttribute("roles", List.of(Role.ADMIN, Role.LIBRARIAN));   // không có Độc giả
        return "index";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String username,
                       @RequestParam String fullName,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) String password,
                       @RequestParam String role,
                       @RequestParam(defaultValue = "true") boolean active,
                       RedirectAttributes ra) {
        try {
            accountService.save(id, username, fullName, email, password, role, active);
            ra.addFlashAttribute("success", "Lưu tài khoản thành công");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            accountService.toggleActive(id, principal.getName());
            ra.addFlashAttribute("success", "Đã đổi trạng thái tài khoản");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            accountService.delete(id, principal.getName());
            ra.addFlashAttribute("success", "Đã xóa tài khoản");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts";
    }
}