package qltv.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Đăng nhập, đăng ký, trang chủ. */
@Controller
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("view", "login");
        return "index";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("view", "register");
        return "index";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String fullName,
                           @RequestParam(required = false) String email,
                           @RequestParam String readerCode,
                           @RequestParam String password,
                           @RequestParam String confirm,
                           Model model) {
        try {
            accountService.register(username, fullName, email, password, confirm, readerCode);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("view", "register");
            model.addAttribute("error", e.getMessage());
            return "index";                 // giữ lại dữ liệu đã nhập (trừ mật khẩu)
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("view", "home");
        return "index";
    }

    // ---- Các chức năng chưa làm: hiện trang "đang phát triển" ----
    // Khi làm xong chức năng nào thì xóa dòng tương ứng ở đây và tạo controller riêng.
    @GetMapping("/books")
    public String books(Model model) { return soon(model, "books", "Quản lý sách"); }

    @GetMapping("/borrows")
    public String borrows(Model model) { return soon(model, "borrows", "Quản lý phiếu mượn"); }

    @GetMapping("/resources")
    public String resources(Model model) { return soon(model, "resources", "Tài liệu số"); }

    @GetMapping("/statistics")
    public String statistics(Model model) { return soon(model, "statistics", "Thống kê"); }

    private String soon(Model model, String view, String title) {
        model.addAttribute("view", view);
        model.addAttribute("title", title);
        return "index";
    }
}
