package qltv.reader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public ReaderService(ReaderRepository readerRepository,
                         AccountRepository accountRepository,
                         PasswordEncoder passwordEncoder) {
        this.readerRepository = readerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<Reader> search(String q, int page) {
        String keyword = q == null ? "" : q.trim();
        return readerRepository.search(keyword, PageRequest.of(page, 10, Sort.by("id").descending()));
    }

    /**
     * Thêm (id == null): tạo luôn tài khoản đăng nhập cho độc giả (cần username + password).
     * Sửa (id != null): username không đổi; password để trống thì giữ nguyên.
     */
    @Transactional
    public void save(Long id, String readerCode, String fullName, String email, String phone,
                     String address, LocalDate birthDate, boolean active,
                     String username, String password) {
        readerCode = readerCode.trim();
        checkEmail(email);

        readerRepository.findByReaderCode(readerCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Mã độc giả đã tồn tại");
            }
        });

        boolean hasPassword = password != null && !password.isBlank();
        Reader r;
        Account acc;

        if (id == null) {                                       // thêm mới + tạo tài khoản
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Tên đăng nhập không được để trống");
            }
            username = username.trim();
            if (username.length() < 4) {
                throw new IllegalArgumentException("Tên đăng nhập tối thiểu 4 ký tự");
            }
            if (!username.matches("[A-Za-z0-9_.]+")) {
                throw new IllegalArgumentException("Tên đăng nhập chỉ gồm chữ không dấu, số, dấu . và _");
            }
            if (accountRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
            }
            if (!hasPassword) {
                throw new IllegalArgumentException("Mật khẩu không được để trống");
            }
            acc = new Account();
            acc.setUsername(username);
            acc.setRole(Role.READER);
            acc.setActive(true);
            r = new Reader();
            r.setAccount(acc);
        } else {                                                // sửa
            r = readerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả"));
            acc = r.getAccount();
            if (acc == null) {
                throw new IllegalArgumentException("Độc giả chưa có tài khoản, hãy chạy script cập nhật database");
            }
        }

        if (hasPassword) {
            if (password.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
            }
            acc.setPassword(passwordEncoder.encode(password));
        }

        r.setReaderCode(readerCode);
        r.setFullName(fullName.trim());
        r.setEmail(blankToNull(email));
        r.setPhone(blankToNull(phone));
        r.setAddress(blankToNull(address));
        r.setBirthDate(birthDate);
        r.setActive(active);               // trạng thái độc giả, tách riêng với trạng thái tài khoản

        // Đồng bộ họ tên + email sang tài khoản để dữ liệu không bị lệch
        acc.setFullName(r.getFullName());
        acc.setEmail(r.getEmail());

        if (id == null) {
            accountRepository.save(acc);    // lưu tài khoản trước
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

        // Xóa luôn tài khoản đăng nhập của độc giả
        if (acc != null && acc.getRole() == Role.READER) {
            accountRepository.delete(acc);
        }
    }

    private void checkEmail(String email) {
        if (email != null && !email.isBlank()
                && !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
