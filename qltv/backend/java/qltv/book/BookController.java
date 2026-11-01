package qltv.book;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute Book book,
            @RequestParam String authorName,
            @RequestParam String publisherName,
            RedirectAttributes redirectAttributes) {

        boolean isUpdate = book.getId() != null;

        try {
            bookService.save(book, authorName, publisherName);

            redirectAttributes.addFlashAttribute(
                    "success",
                    isUpdate ? "Cập nhật sách thành công!" : "Thêm sách thành công!"
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể lưu sách. Vui lòng kiểm tra dữ liệu."
            );
        }

        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            bookService.delete(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Xóa sách thành công!"
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không thể xóa sách. Có thể sách đang được sử dụng."
            );
        }

        return "redirect:/books";
    }
}