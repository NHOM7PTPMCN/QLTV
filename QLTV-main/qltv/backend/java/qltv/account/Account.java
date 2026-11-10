package qltv.account;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;        // da ma hoa BCrypt

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    // Lưu số: 0 = ADMIN, 1 = LIBRARIAN, 2 = READER
    @Column(name = "role_code", nullable = false)
    private int roleCode = Role.READER.getCode();

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getRoleCode() { return roleCode; }
    public Role getRole() { return Role.fromCode(roleCode); }
    public void setRole(Role role) { this.roleCode = role.getCode(); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
