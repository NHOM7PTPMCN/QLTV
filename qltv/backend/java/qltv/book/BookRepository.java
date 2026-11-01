
package qltv.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            select b from Book b
            where (:q = '' or lower(b.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(b.isbn, '')) like lower(concat('%', :q, '%')))
              and (:categoryId is null or b.categoryId = :categoryId)
              and (:publishYear is null or b.publishYear = :publishYear)
            """)
    Page<Book> search(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("publishYear") Integer publishYear,
            Pageable pageable
    );
}
