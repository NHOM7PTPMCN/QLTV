package qltv.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import qltv.account.Account;
import qltv.account.AccountRepository;
import qltv.account.Role;

/** Tu tao tai khoan admin / admin123 o lan chay dau tien. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            Account admin = new Account();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Quản trị viên");
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            accountRepository.save(admin);
        }
    }
}
