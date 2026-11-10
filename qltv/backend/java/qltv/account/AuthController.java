package qltv.account;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

/** Đăng nhập, đăng ký, quên mật khẩu, thông tin cá nhân, trang chủ. */
@Controller
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ---------- Đăng nhập (KAN-4; đăng xuất do Spring Security xử lý ở POST /logout) ----------
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("view", "login");
        return "index";
    }

    // ---------- Đăng ký (KAN-3) ----------
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

    // ---------- Quên mật khẩu (KAN-5) ----------
    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("view", "forgot");
        return "index";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String username,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String confirm,
                         Model model) {
        try {
            accountService.resetPassword(username, email, password, confirm);
            return "redirect:/login?reset";
        } catch (IllegalArgumentException e) {
            model.addAttribute("view", "forgot");
            model.addAttribute("error", e.getMessage());
            return "index";
        }
    }

    // ---------- Thông tin cá nhân (KAN-7) và đổi mật khẩu (KAN-6) ----------
    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        Account acc = accountService.findByUsername(principal.getName());
        model.addAttribute("view", "profile");
        model.addAttribute("account", acc);
        model.addAttribute("reader", accountService.findReaderOf(acc));   // null nếu là nhân viên
        return "index";
    }

    @PostMapping("/profile")
    public String updateProfile(Principal principal,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                                RedirectAttributes ra) {
        try {
            accountService.updateProfile(principal.getName(), fullName, email, phone, address, birthDate);
            ra.addFlashAttribute("success", "Cập nhật thông tin cá nhân thành công");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(Principal principal,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirm,
                                 RedirectAttributes ra) {
        try {
            accountService.changePassword(principal.getName(), oldPassword, newPassword, confirm);
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ---------- Trang chủ ----------
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
