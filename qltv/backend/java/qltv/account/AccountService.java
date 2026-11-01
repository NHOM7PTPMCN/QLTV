package qltv.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qltv.reader.Reader;
import qltv.reader.ReaderRepository;

/** Vua xu ly tai khoan, vua dung de Spring Security kiem tra dang nhap. */
@Service
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final ReaderRepository readerRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository,
                          ReaderRepository readerRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.readerRepository = readerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===================== ĐĂNG NHẬP =====================
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account a = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return User.withUsername(a.getUsername())
                .password(a.getPassword())
                .roles(a.getRole().name())          // ADMIN -> ROLE_ADMIN
                .disabled(!a.isActive())
                .build();
    }

    // ===================== ĐĂNG KÝ (độc giả tự đăng ký) =====================
    @Transactional
    public void register(String username, String fullName, String email,
                         String password, String confirm, String readerCode) {
        username = username.trim();
        readerCode = readerCode.trim();

        if (username.length() < 4) {
            throw new IllegalArgumentException("Tên đăng nhập tối thiểu 4 ký tự");
        }
        if (!username.matches("[A-Za-z0-9_.]+")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ gồm chữ không dấu, số, dấu . và _");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
        }
        if (accountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        Reader reader = readerRepository.findByReaderCode(readerCode).orElse(null);
        if (reader != null && reader.getAccount() != null) {
            throw new IllegalArgumentException("Mã độc giả này đã có tài khoản");
        }

        Account acc = new Account();
        acc.setUsername(username);
        acc.setPassword(passwordEncoder.encode(password));
        acc.setFullName(fullName.trim());
        acc.setEmail(blankToNull(email));
        acc.setRole(Role.READER);
        acc.setActive(true);
        accountRepository.save(acc);

        if (reader == null) {                       // chưa có hồ sơ độc giả thì tạo mới
            reader = new Reader();
            reader.setReaderCode(readerCode);
            reader.setFullName(fullName.trim());
            reader.setEmail(blankToNull(email));
        }
        reader.setAccount(acc);                     // gắn tài khoản vào hồ sơ độc giả
        readerRepository.save(reader);
    }

    // ===================== QUẢN LÝ TÀI KHOẢN (admin) =====================
    // Trang này chỉ quản lý Quản trị viên và Thủ thư. Tài khoản Độc giả được quản lý
    // qua hồ sơ độc giả (đăng ký / trang Quản lý độc giả).

    public Page<Account> search(String q, int page) {
        String keyword = q == null ? "" : q.trim();
        return accountRepository.searchStaff(keyword, Role.READER.getCode(),
                PageRequest.of(page, 10, Sort.by("id").descending()));
    }

    @Transactional
    public void save(Long id, String username, String fullName, String email,
                     String password, String role, boolean active) {
        Role newRole = parseStaffRole(role);
        boolean hasPassword = password != null && !password.isBlank();
        Account acc;

        if (id == null) {                                       // thêm mới
            username = username.trim();
            if (accountRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
            }
            if (!hasPassword) {
                throw new IllegalArgumentException("Mật khẩu không được để trống");
            }
            acc = new Account();
            acc.setUsername(username);
        } else {                                                // sửa (không đổi tên đăng nhập)
            acc = findStaff(id);
        }

        if (hasPassword) {
            if (password.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
            }
            acc.setPassword(passwordEncoder.encode(password));      // BCrypt
        }

        acc.setFullName(fullName.trim());
        acc.setEmail(blankToNull(email));
        acc.setRole(newRole);
        acc.setActive(active);
        accountRepository.save(acc);
    }

    @Transactional
    public void toggleActive(Long id, String currentUsername) {
        Account acc = findStaff(id);
        if (acc.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Không thể khóa chính tài khoản đang đăng nhập");
        }
        acc.setActive(!acc.isActive());
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        Account acc = findStaff(id);
        if (acc.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Không thể xóa chính tài khoản đang đăng nhập");
        }
        readerRepository.findByAccount(acc).ifPresent(r -> r.setAccount(null));   // gỡ liên kết (nếu có)
        accountRepository.delete(acc);
    }

    /** Chỉ chấp nhận ADMIN hoặc LIBRARIAN. */
    private Role parseStaffRole(String role) {
        Role r;
        try {
            r = Role.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }
        if (r == Role.READER) {
            throw new IllegalArgumentException("Vai trò chỉ được chọn Quản trị viên hoặc Thủ thư");
        }
        return r;
    }

    /** Tìm tài khoản nhân viên; tài khoản độc giả không được thao tác ở trang này. */
    private Account findStaff(Long id) {
        Account acc = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if (acc.getRole() == Role.READER) {
            throw new IllegalArgumentException("Tài khoản độc giả được quản lý ở trang Quản lý độc giả");
        }
        return acc;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}