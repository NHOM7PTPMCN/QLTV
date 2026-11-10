package qltv.book;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final JdbcTemplate jdbcTemplate;

    public BookService(BookRepository bookRepository, JdbcTemplate jdbcTemplate) {
        this.bookRepository = bookRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<Book> search(String q, Long categoryId,
                             Integer publishYear, int page) {
        String keyword = q == null ? "" : q.trim();

        if (page < 0) {
            page = 0;
        }

        return bookRepository.search(
                keyword,
                categoryId,
                publishYear,
                PageRequest.of(page, 10, Sort.by("id").ascending())
        );
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy sách"));
    }

    @Transactional
    public void save(Book book, String authorName, String publisherName) {

        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên sách không được để trống");
        }

        if (authorName == null || authorName.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên tác giả");
        }

        if (publisherName == null || publisherName.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nhà xuất bản");
        }

        if (book.getCategoryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thể loại");
        }

        if (book.getPublishYear() <= 0) {
            throw new IllegalArgumentException("Năm xuất bản không hợp lệ");
        }

        if (book.getId() != null) {
            Book existingBook = findById(book.getId());
            book.setIsbn(existingBook.getIsbn());
        } else {
            book.setIsbn(generateIsbn());
        }

        Long authorId = findOrCreate("authors", authorName);
        Long publisherId = findOrCreate("publishers", publisherName);

        book.setAuthorId(authorId);
        book.setPublisherId(publisherId);
        book.setTitle(book.getTitle().trim());

        bookRepository.save(book);
    }

    private Long findOrCreate(String table, String name) {
        String cleanName = name.trim();

        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM " + table
                        + " WHERE LOWER(name) = LOWER(?) LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                cleanName
        );

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + table + " (name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, cleanName);
            return statement;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private String generateIsbn() {
        Long maxIsbn = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CAST(isbn AS UNSIGNED)), 9780000000000) "
                        + "FROM books WHERE isbn REGEXP '^[0-9]+$'",
                Long.class
        );

        return String.valueOf(maxIsbn + 1);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findById(id);
        bookRepository.delete(book);
    }
}