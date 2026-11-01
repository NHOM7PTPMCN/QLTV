package qltv.reader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qltv.account.Account;
import qltv.account.AccountRepository;
import qltv.account.Role;

import java.time.LocalDate;

@Service
public class ReaderService {

    private final ReaderRepository readerRepository;
    private final AccountRepository accountRepository;

    public ReaderService(ReaderRepository readerRepository, AccountRepository accountRepository) {
        this.readerRepository = readerRepository;
        this.accountRepository = accountRepository;
    }

    public Page<Reader> search(String q, int page) {
        String keyword = q == null ? "" : q.trim();
        return readerRepository.search(keyword, PageRequest.of(page, 10, Sort.by("id").descending()));
    }

    @Transactional
    public void save(Long id, String readerCode, String fullName, String email, String phone,
                     String address, LocalDate birthDate, boolean active) {
        readerCode = readerCode.trim();

        readerRepository.findByReaderCode(readerCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Mã độc giả đã tồn tại");
            }
        });

        Reader r = (id == null)
                ? new Reader()
                : readerRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả"));

        r.setReaderCode(readerCode);
        r.setFullName(fullName.trim());
        r.setEmail(blankToNull(email));
        r.setPhone(blankToNull(phone));
        r.setAddress(blankToNull(address));
        r.setBirthDate(birthDate);
        r.setActive(active);               // trạng thái độc giả, tách riêng với trạng thái tài khoản

        // Đồng bộ họ tên + email sang tài khoản đã liên kết để dữ liệu không bị lệch
        Account acc = r.getAccount();
        if (acc != null) {
            acc.setFullName(r.getFullName());
            acc.setEmail(r.getEmail());
        }
        readerRepository.save(r);
    }

    @Transactional
    public void delete(Long id) {
        Reader r = readerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả"));
        Account acc = r.getAccount();

        readerRepository.delete(r);
        readerRepository.flush();          // đang có phiếu mượn thì lỗi khóa ngoại được ném ra ở đây

        // Tài khoản độc giả không hiện ở trang Quản lý tài khoản nên xóa cùng hồ sơ
        if (acc != null && acc.getRole() == Role.READER) {
            accountRepository.delete(acc);
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}