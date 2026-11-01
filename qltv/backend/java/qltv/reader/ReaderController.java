package qltv.reader;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/readers")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Reader> data = readerService.search(q, page);
        model.addAttribute("view", "readers");
        model.addAttribute("data", data);
        model.addAttribute("q", q);
        return "index";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String readerCode,
                       @RequestParam String fullName,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) String address,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                       @RequestParam(defaultValue = "true") boolean active,
                       RedirectAttributes ra) {
        try {
            readerService.save(id, readerCode, fullName, email, phone, address, birthDate, active);
            ra.addFlashAttribute("success", "Lưu độc giả thành công");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/readers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            readerService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa độc giả");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Không thể xóa: độc giả này đang có phiếu mượn");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/readers";
    }
}