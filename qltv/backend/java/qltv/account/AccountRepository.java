package qltv.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Tìm tài khoản nhân viên (bỏ qua vai trò excludedRole, ở đây là Độc giả). */
    @Query("""
           select a from Account a
           where a.roleCode <> :excludedRole
             and (lower(a.username) like lower(concat('%', :q, '%'))
               or lower(a.fullName) like lower(concat('%', :q, '%'))
               or lower(coalesce(a.email, '')) like lower(concat('%', :q, '%')))
           """)
    Page<Account> searchStaff(@Param("q") String q,
                              @Param("excludedRole") int excludedRole,
                              Pageable pageable);
}