package qltv.reader;

import jakarta.persistence.*;
import qltv.account.Account;

import java.time.LocalDate;

@Entity
@Table(name = "readers")
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String readerCode;          // ví dụ SV2022096

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    private LocalDate birthDate;

    private boolean active = true;

    private LocalDate createdAt;

    // Tài khoản đăng nhập của độc giả (có thể chưa có)
    @OneToOne
    @JoinColumn(name = "account_id", unique = true)
    private Account account;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReaderCode() { return readerCode; }
    public void setReaderCode(String readerCode) { this.readerCode = readerCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
