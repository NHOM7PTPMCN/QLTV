package qltv.reader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import qltv.account.Account;

import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {

    Optional<Reader> findByReaderCode(String readerCode);

    Optional<Reader> findByAccount(Account account);

    @Query("""
           select r from Reader r
           where lower(r.readerCode) like lower(concat('%', :q, '%'))
              or lower(r.fullName) like lower(concat('%', :q, '%'))
              or lower(coalesce(r.email, '')) like lower(concat('%', :q, '%'))
           """)
    Page<Reader> search(@Param("q") String q, Pageable pageable);
}
