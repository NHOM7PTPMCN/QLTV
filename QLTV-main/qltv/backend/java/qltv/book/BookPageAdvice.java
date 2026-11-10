package qltv.book;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class BookPageAdvice {

    private final BookService bookService;
    private final JdbcTemplate jdbcTemplate;

    public BookPageAdvice(BookService bookService, JdbcTemplate jdbcTemplate) {
        this.bookService = bookService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @ModelAttribute
    public void addBookData(HttpServletRequest request, Model model) {

        if (!request.getRequestURI().equals("/books")) {
            return;
        }

        String q = request.getParameter("q");
        String categoryParam = request.getParameter("categoryId");
        String yearParam = request.getParameter("publishYear");
        String pageParam = request.getParameter("page");
        String editIdParam = request.getParameter("editId");

        Long categoryId = null;
        Integer publishYear = null;
        int page = 0;

        try {
            if (categoryParam != null && !categoryParam.isBlank()) {
                categoryId = Long.parseLong(categoryParam);
            }

            if (yearParam != null && !yearParam.isBlank()) {
                publishYear = Integer.parseInt(yearParam);
            }

            if (pageParam != null && !pageParam.isBlank()) {
                page = Integer.parseInt(pageParam);
            }

        } catch (NumberFormatException e) {
            page = 0;
            categoryId = null;
            publishYear = null;
        }

        Page<Book> data = bookService.search(q, categoryId, publishYear, page);

        model.addAttribute("data", data);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("publishYear", publishYear);

        List<Map<String, Object>> authors =
                jdbcTemplate.queryForList("SELECT id, name FROM authors");

        List<Map<String, Object>> categories =
                jdbcTemplate.queryForList("SELECT id, name FROM categories");

        List<Map<String, Object>> publishers =
                jdbcTemplate.queryForList("SELECT id, name FROM publishers");

        model.addAttribute("authors", authors);
        model.addAttribute("categories", categories);
        model.addAttribute("publishers", publishers);

        model.addAttribute("authorNames", createNameMap(authors));
        model.addAttribute("categoryNames", createNameMap(categories));
        model.addAttribute("publisherNames", createNameMap(publishers));

        if (editIdParam != null && !editIdParam.isBlank()) {
            try {
                Long editId = Long.parseLong(editIdParam);
                Book editingBook = bookService.findById(editId);

                model.addAttribute("editingBook", editingBook);

                String authorName = jdbcTemplate.queryForObject(
                        "SELECT name FROM authors WHERE id = ?",
                        String.class,
                        editingBook.getAuthorId()
                );

                String publisherName = jdbcTemplate.queryForObject(
                        "SELECT name FROM publishers WHERE id = ?",
                        String.class,
                        editingBook.getPublisherId()
                );

                model.addAttribute("editingAuthorName", authorName);
                model.addAttribute("editingPublisherName", publisherName);

            } catch (NumberFormatException e) {
                model.addAttribute("editingBook", null);
            }
        }
    }

    private Map<Long, String> createNameMap(List<Map<String, Object>> rows) {

        Map<Long, String> result = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String name = (String) row.get("name");

            result.put(id, name);
        }

        return result;
    }
}